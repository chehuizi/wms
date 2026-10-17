package com.wms.module.outbound.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.OutboundOrderQuery;
import com.wms.dto.OutboundOrderSaveRequest;
import com.wms.dto.PickTaskQuery;
import com.wms.dto.ShipRequest;
import com.wms.entity.Allocation;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundItem;
import com.wms.entity.OutboundOrder;
import com.wms.entity.PickTask;
import com.wms.entity.Serial;
import com.wms.entity.Shipment;
import com.wms.entity.Sku;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.PickTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.mapper.ShipmentMapper;
import com.wms.mapper.SkuMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.common.StatusTransitions;
import com.wms.module.outbound.OutboundService;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.vo.AllocationVO;
import com.wms.vo.OutboundOrderDetailVO;
import com.wms.vo.OutboundOrderVO;
import com.wms.vo.PickTaskVO;
import com.wms.vo.ShipmentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 出库订单实现，见契约第 16 节。
 * <p>
 * 库存写操作一律委托 {@link StockService}，本类只负责单据状态与分配/拣货/发货的业务编排。
 */
@Service
public class OutboundServiceImpl implements OutboundService {

    /** 状态：1=草稿 2=待分配 3=已分配 4=拣货中 5=待发货 6=已发货 7=已取消 */
    private static final int STATUS_DRAFT = 1;
    private static final int STATUS_PENDING = 2;
    private static final int STATUS_ALLOCATED = 3;
    private static final int STATUS_PICKING = 4;
    private static final int STATUS_WAITING_SHIP = 5;
    private static final int STATUS_SHIPPED = 6;
    private static final int STATUS_CANCELLED = 7;

    /** 分配状态：1=已锁定 2=拣货中 3=已拣货 4=已发货 5=已释放 */
    private static final int ALLOC_LOCKED = 1;
    private static final int ALLOC_PICKING = 2;
    private static final int ALLOC_PICKED = 3;

    private final OutboundOrderMapper outboundOrderMapper;
    private final InventoryMapper inventoryMapper;
    private final PickTaskMapper pickTaskMapper;
    private final ShipmentMapper shipmentMapper;
    private final SkuMapper skuMapper;
    private final SerialMapper serialMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockService stockService;
    private final DocNoGenerator docNoGenerator;

    public OutboundServiceImpl(OutboundOrderMapper outboundOrderMapper,
                               InventoryMapper inventoryMapper,
                               PickTaskMapper pickTaskMapper,
                               ShipmentMapper shipmentMapper,
                               SkuMapper skuMapper,
                               SerialMapper serialMapper,
                               WarehouseMapper warehouseMapper,
                               StockService stockService,
                               DocNoGenerator docNoGenerator) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.inventoryMapper = inventoryMapper;
        this.pickTaskMapper = pickTaskMapper;
        this.shipmentMapper = shipmentMapper;
        this.skuMapper = skuMapper;
        this.serialMapper = serialMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockService = stockService;
        this.docNoGenerator = docNoGenerator;
    }

    @Override
    public PageResult<OutboundOrderVO> page(OutboundOrderQuery query) {
        long total = outboundOrderMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(outboundOrderMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public OutboundOrderDetailVO getById(Long id) {
        OutboundOrderVO vo = outboundOrderMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        OutboundOrderDetailVO detail = new OutboundOrderDetailVO();
        BeanUtils.copyProperties(vo, detail);
        detail.setItems(outboundOrderMapper.selectItemVOs(id));
        detail.setAllocations(outboundOrderMapper.selectAllocationVOs(id));
        return detail;
    }

    @Override
    @Transactional
    public OutboundOrderDetailVO create(OutboundOrderSaveRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + request.getWarehouseId());
        }
        validateItems(request);
        OutboundOrder order = new OutboundOrder();
        BeanUtils.copyProperties(request, order);
        order.setOrderNo(docNoGenerator.next(DocNoGenerator.OUTBOUND_ORDER));
        order.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        order.setStatus(STATUS_DRAFT);
        order.setDeleted(0);
        outboundOrderMapper.insert(order);
        insertItems(order.getId(), request);
        return getById(order.getId());
    }

    @Override
    @Transactional
    public OutboundOrderDetailVO update(Long id, OutboundOrderSaveRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + request.getWarehouseId());
        }
        validateItems(request);
        OutboundOrder order = requireStatus(id, STATUS_DRAFT);
        BeanUtils.copyProperties(request, order);
        order.setId(id);
        order.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        outboundOrderMapper.update(order);
        outboundOrderMapper.deleteItems(id);
        insertItems(id, request);
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireStatus(id, STATUS_DRAFT);
        outboundOrderMapper.deleteItems(id);
        outboundOrderMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public OutboundOrderDetailVO submit(Long id) {
        OutboundOrder order = requireStatus(id, STATUS_DRAFT);
        StatusTransitions.requireOutbound(order.getStatus(), STATUS_PENDING, order.getOrderNo());
        outboundOrderMapper.updateStatus(id, STATUS_PENDING);
        return getById(id);
    }

    @Override
    @Transactional
    public OutboundOrderDetailVO cancel(Long id) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_SHIPPED) {
            throw BizException.orderStatusNotAllowed("已发货订单不能取消: " + order.getOrderNo());
        }
        // 先自动释放锁定，再置为已取消
        if (order.getStatus() != null
                && (order.getStatus() == STATUS_PENDING
                || order.getStatus() == STATUS_ALLOCATED
                || order.getStatus() == STATUS_PICKING)) {
            releaseLockedAllocations(order);
        }
        StatusTransitions.requireOutbound(order.getStatus(), STATUS_CANCELLED, order.getOrderNo());
        outboundOrderMapper.updateStatus(id, STATUS_CANCELLED);
        return getById(id);
    }

    // ---------------------------------------------------------------- 分配

    /**
     * 分配库存并锁定。契约第 16.2 条：FIFO 取可用量，全成或全败。
     * <p>
     * 先对所有明细做一次可用量校验（此时已 FOR UPDATE 锁住候选库存行），
     * 任何一条不足立即抛 40903；全部通过后才开始写锁定与流水，
     * 因此不会留下「已锁定一半」的残留。
     * 按 skuId 升序处理，避免多 SKU 并发分配互相死锁。
     */
    @Override
    @Transactional
    public OutboundOrderDetailVO allocate(Long id) {
        OutboundOrder order = requireStatus(id, STATUS_PENDING);
        List<OutboundItem> items = new ArrayList<>(outboundOrderMapper.selectItems(id));
        items.sort(Comparator.comparing(OutboundItem::getSkuId));

        BizRef biz = BizRef.of(StockService.BIZ_OUTBOUND, order.getOrderNo());
        List<Plan> plans = new ArrayList<>();
        boolean allAllocated = true;

        for (OutboundItem item : items) {
            Sku sku = requireSku(item.getSkuId());
            BigDecimal remaining = item.getOrderedQty().subtract(nvl(item.getAllocatedQty()));
            if (remaining.signum() <= 0) {
                continue;
            }
            List<Inventory> candidates =
                    inventoryMapper.selectAvailableForSkuFifo(order.getWarehouseId(), item.getSkuId());
            BigDecimal available = candidates.stream()
                    .map(inv -> nvl(inv.getOnHandQty()).subtract(nvl(inv.getLockedQty())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(remaining) < 0) {
                // 可用量不足：直接抛错，此时还没写任何锁定，靠异常回滚保证不留残留
                throw BizException.qtyNotEnough(sku.getCode() + " 需 " + remaining
                        + "，可用 " + available + "，缺 " + remaining.subtract(available));
            }
            allAllocated = false;
            plans.add(consumePlan(order.getId(), item, remaining, candidates, biz));
        }

        // 校验全部通过后统一落库
        for (Plan plan : plans) {
            for (Allocation alloc : plan.allocations()) {
                outboundOrderMapper.insertAllocation(alloc);
                outboundOrderMapper.addAllocatedQty(plan.item().getId(), alloc.getAllocatedQty());
            }
        }
        if (!allAllocated) {
            StatusTransitions.requireOutbound(order.getStatus(), STATUS_ALLOCATED, order.getOrderNo());
            outboundOrderMapper.updateStatus(id, STATUS_ALLOCATED);
        }
        return getById(id);
    }

    /** 按 FIFO 从候选行里切出分配方案 */
    private Plan consumePlan(Long outboundId, OutboundItem item, BigDecimal remaining,
                             List<Inventory> candidates, BizRef biz) {
        List<Allocation> allocations = new ArrayList<>();
        BigDecimal need = remaining;
        for (Inventory inv : candidates) {
            if (need.signum() <= 0) {
                break;
            }
            BigDecimal canTake = nvl(inv.getOnHandQty()).subtract(nvl(inv.getLockedQty()));
            if (canTake.signum() <= 0) {
                continue;
            }
            BigDecimal take = canTake.min(need);
            Allocation alloc = new Allocation();
            alloc.setOutboundId(outboundId);
            alloc.setOutboundItemId(item.getId());
            alloc.setInventoryId(inv.getId());
            alloc.setWarehouseId(inv.getWarehouseId());
            alloc.setLocationId(inv.getLocationId());
            alloc.setSkuId(inv.getSkuId());
            alloc.setBatchNo(inv.getBatchNo());
            alloc.setAllocatedQty(take);
            alloc.setPickedQty(BigDecimal.ZERO);
            alloc.setShippedQty(BigDecimal.ZERO);
            alloc.setStatus(ALLOC_LOCKED);
            allocations.add(alloc);

            // 同一事务内锁库存 + 写流水 movementType = 7
            stockService.changeLocked(inv, take, MovementType.LOCK, biz, null, "出库分配锁定");
            need = need.subtract(take);
        }
        return new Plan(item, allocations);
    }

    @Override
    @Transactional
    public OutboundOrderDetailVO releaseAllocation(Long id) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        if (order.getStatus() != STATUS_ALLOCATED && order.getStatus() != STATUS_PICKING) {
            throw BizException.orderStatusNotAllowed(
                    "只有已分配或拣货中的订单可以释放锁定，当前状态: " + order.getStatus());
        }
        releaseLockedAllocations(order);
        // 契约第 16.6 节要求释放后回到「待分配」。这与第 12.2 节状态机表
        // （3 -> 只能 4 / 7）不一致，此处以第 16.6 节的业务描述为准。
        outboundOrderMapper.updateStatus(id, STATUS_PENDING);
        return getById(id);
    }

    /** 释放所有 status ∈ {1,2} 的分配：解锁、写流水 8、累减明细的已分配/已拣数量 */
    private void releaseLockedAllocations(OutboundOrder order) {
        List<Allocation> allocations =
                outboundOrderMapper.selectAllocationsByStatus(order.getId(), List.of(ALLOC_LOCKED, ALLOC_PICKING));
        BizRef biz = BizRef.of(StockService.BIZ_OUTBOUND, order.getOrderNo());
        for (Allocation alloc : allocations) {
            BigDecimal picked = nvl(alloc.getPickedQty());
            BigDecimal releaseQty = nvl(alloc.getAllocatedQty()).subtract(picked);
            if (releaseQty.signum() > 0) {
                Inventory inventory = inventoryMapper.selectByIdForUpdate(alloc.getInventoryId());
                if (inventory != null) {
                    stockService.changeLocked(inventory, releaseQty.negate(), MovementType.UNLOCK,
                            biz, null, "释放分配锁定");
                }
            }
            if (releaseQty.signum() != 0) {
                outboundOrderMapper.addAllocatedQty(alloc.getOutboundItemId(), releaseQty.negate());
            }
            if (picked.signum() > 0) {
                outboundOrderMapper.addPickedQty(alloc.getOutboundItemId(), picked.negate());
            }
            pickTaskMapper.cancelUnfinishedByAllocation(alloc.getId());
            outboundOrderMapper.markAllocationStatus(alloc.getId(), 5);
        }
    }

    // ---------------------------------------------------------------- 拣货

    @Override
    @Transactional
    public OutboundOrderDetailVO createPickTasks(Long id) {
        OutboundOrder order = requireStatus(id, STATUS_ALLOCATED);
        List<Allocation> allocations =
                outboundOrderMapper.selectAllocationsByStatus(id, List.of(ALLOC_LOCKED));
        if (allocations.isEmpty()) {
            throw BizException.orderStatusNotAllowed("没有待拣货的分配记录: " + order.getOrderNo());
        }
        StatusTransitions.requireOutbound(order.getStatus(), STATUS_PICKING, order.getOrderNo());
        int sequence = 1;
        for (Allocation alloc : allocations) {
            PickTask task = new PickTask();
            task.setTaskNo(docNoGenerator.next(DocNoGenerator.PICK_TASK));
            task.setOutboundId(id);
            task.setAllocationId(alloc.getId());
            task.setSkuId(alloc.getSkuId());
            task.setFromLocationId(alloc.getLocationId());
            task.setSequence(sequence++);
            task.setRequiredQty(alloc.getAllocatedQty());
            task.setPickedQty(BigDecimal.ZERO);
            task.setStatus(1);
            pickTaskMapper.insert(task);
            outboundOrderMapper.markAllocationStatus(alloc.getId(), ALLOC_PICKING);
        }
        outboundOrderMapper.updateStatus(id, STATUS_PICKING);
        return getById(id);
    }

    @Override
    public List<PickTaskVO> pickTasks(Long id) {
        if (outboundOrderMapper.selectById(id) == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        PickTaskQuery query = new PickTaskQuery();
        query.setOutboundId(id);
        query.setSize(200);
        return pickTaskMapper.selectPage(query);
    }

    // ---------------------------------------------------------------- 发货

    @Override
    @Transactional
    public ShipmentVO ship(Long id, ShipRequest request) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_WAITING_SHIP) {
            throw BizException.orderStatusNotAllowed(
                    "只有待发货的订单可以发货，当前状态: " + order.getStatus());
        }
        StatusTransitions.requireOutbound(order.getStatus(), STATUS_SHIPPED, order.getOrderNo());

        BizRef biz = BizRef.of(StockService.BIZ_OUTBOUND, order.getOrderNo());
        List<Allocation> allocations =
                outboundOrderMapper.selectAllocationsByStatus(id, List.of(ALLOC_PICKED, ALLOC_PICKING));
        for (Allocation alloc : allocations) {
            BigDecimal shipQty = nvl(alloc.getPickedQty());
            if (shipQty.signum() <= 0) {
                continue;
            }
            Inventory inventory = inventoryMapper.selectByIdForUpdate(alloc.getInventoryId());
            if (inventory == null) {
                throw BizException.notFound("分配记录关联的库存不存在: " + alloc.getInventoryId());
            }
            // 在库量与锁定量同时扣减，只写一条 movementType = 2 的流水
            stockService.change(inventory, shipQty.negate(), shipQty.negate(),
                    MovementType.OUTBOUND_SHIP, biz, request.getOperator(), "出库发货");

            if (inventory.getSerialNo() != null && !inventory.getSerialNo().isBlank()) {
                Serial serial = serialMapper.selectBySerialNo(inventory.getSerialNo());
                if (serial != null) {
                    serialMapper.markOutbound(serial.getId());
                }
            }
            outboundOrderMapper.addShippedQty(alloc.getOutboundItemId(), shipQty);
            outboundOrderMapper.addAllocationShippedQty(alloc.getId(), shipQty);
            outboundOrderMapper.markAllocationStatus(alloc.getId(), 4);
        }

        Shipment shipment = new Shipment();
        shipment.setShipmentNo(docNoGenerator.next(DocNoGenerator.SHIPMENT));
        shipment.setOutboundId(id);
        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNo(request.getTrackingNo());
        shipment.setOperator(request.getOperator());
        shipment.setRemark(request.getRemark());
        shipmentMapper.insert(shipment);
        outboundOrderMapper.markShipped(id);

        ShipmentVO vo = new ShipmentVO();
        BeanUtils.copyProperties(shipment, vo);
        return vo;
    }

    @Override
    public List<ShipmentVO> shipments(Long id) {
        if (outboundOrderMapper.selectById(id) == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        return shipmentMapper.selectByOutboundId(id);
    }

    @Override
    public List<AllocationVO> allocations(Long id) {
        if (outboundOrderMapper.selectById(id) == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        return outboundOrderMapper.selectAllocationVOs(id);
    }

    // ---------------------------------------------------------------- 内部

    private void validateItems(OutboundOrderSaveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BizException.paramInvalid("明细至少 1 条");
        }
        Set<Long> seen = new HashSet<>();
        for (OutboundOrderSaveRequest.Item item : request.getItems()) {
            if (!seen.add(item.getSkuId())) {
                throw BizException.paramInvalid("同一 SKU 只能出现一次: " + item.getSkuId());
            }
        }
    }

    private void insertItems(Long outboundId, OutboundOrderSaveRequest request) {
        for (OutboundOrderSaveRequest.Item item : request.getItems()) {
            requireSku(item.getSkuId());
            OutboundItem entity = new OutboundItem();
            entity.setOutboundId(outboundId);
            entity.setSkuId(item.getSkuId());
            entity.setOrderedQty(item.getOrderedQty());
            entity.setAllocatedQty(BigDecimal.ZERO);
            entity.setPickedQty(BigDecimal.ZERO);
            entity.setShippedQty(BigDecimal.ZERO);
            outboundOrderMapper.insertItem(entity);
        }
    }

    private OutboundOrder requireStatus(Long id, int required) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("出库订单不存在: " + id);
        }
        if (order.getStatus() == null || order.getStatus() != required) {
            throw BizException.orderStatusNotAllowed(
                    "当前状态 " + order.getStatus() + " 不允许该操作（要求状态 " + required + "）: " + order.getOrderNo());
        }
        return order;
    }

    private Sku requireSku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw BizException.relatedInvalid("商品不存在: " + skuId);
        }
        return sku;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 一条明细的分配方案：明细 + 由它产生的若干条 allocation */
    private record Plan(OutboundItem item, List<Allocation> allocations) {
    }
}
