package com.wms.module.inbound.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderQuery;
import com.wms.dto.InboundOrderSaveRequest;
import com.wms.dto.PutawayTaskQuery;
import com.wms.dto.ReceiptRequest;
import com.wms.entity.InboundItem;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundReceipt;
import com.wms.entity.InboundReceiptItem;
import com.wms.entity.Location;
import com.wms.entity.Serial;
import com.wms.entity.Sku;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.PutawayTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.mapper.SkuMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.common.StatusTransitions;
import com.wms.module.inbound.InboundService;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.vo.InboundItemVO;
import com.wms.vo.InboundOrderDetailVO;
import com.wms.vo.InboundOrderVO;
import com.wms.vo.InboundReceiptItemVO;
import com.wms.vo.InboundReceiptVO;
import com.wms.vo.PutawayTaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 入库到货单实现，见契约第 15 节。
 */
@Service
public class InboundServiceImpl implements InboundService {

    /** 状态：1=草稿 2=待收货 3=部分收货 4=已完成 5=已取消 */
    private static final int STATUS_DRAFT = 1;
    private static final int STATUS_PENDING = 2;
    private static final int STATUS_PARTIAL = 3;
    private static final int STATUS_DONE = 4;

    private final InboundOrderMapper inboundOrderMapper;
    private final PutawayTaskMapper putawayTaskMapper;
    private final SkuMapper skuMapper;
    private final LocationMapper locationMapper;
    private final WarehouseMapper warehouseMapper;
    private final SerialMapper serialMapper;
    private final StockService stockService;
    private final DocNoGenerator docNoGenerator;

    public InboundServiceImpl(InboundOrderMapper inboundOrderMapper,
                              PutawayTaskMapper putawayTaskMapper,
                              SkuMapper skuMapper,
                              LocationMapper locationMapper,
                              WarehouseMapper warehouseMapper,
                              SerialMapper serialMapper,
                              StockService stockService,
                              DocNoGenerator docNoGenerator) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.putawayTaskMapper = putawayTaskMapper;
        this.skuMapper = skuMapper;
        this.locationMapper = locationMapper;
        this.warehouseMapper = warehouseMapper;
        this.serialMapper = serialMapper;
        this.stockService = stockService;
        this.docNoGenerator = docNoGenerator;
    }

    @Override
    public PageResult<InboundOrderVO> page(InboundOrderQuery query) {
        long total = inboundOrderMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<InboundOrderVO> list = inboundOrderMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public InboundOrderDetailVO getById(Long id) {
        InboundOrderVO vo = inboundOrderMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        InboundOrderDetailVO detail = new InboundOrderDetailVO();
        BeanUtils.copyProperties(vo, detail);
        detail.setItems(inboundOrderMapper.selectItemVOs(id));
        return detail;
    }

    @Override
    public PageResult<PutawayTaskVO> pagePutawayTasks(PutawayTaskQuery query) {
        long total = putawayTaskMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(putawayTaskMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    @Transactional
    public InboundOrderDetailVO create(InboundOrderSaveRequest request) {
        requireWarehouse(request.getWarehouseId());
        validateItems(request);

        InboundOrder order = new InboundOrder();
        BeanUtils.copyProperties(request, order);
        order.setOrderNo(docNoGenerator.next(DocNoGenerator.INBOUND_ORDER));
        order.setStatus(STATUS_DRAFT);
        order.setDeleted(0);
        inboundOrderMapper.insert(order);
        insertItems(order.getId(), request);
        return getById(order.getId());
    }

    @Override
    @Transactional
    public InboundOrderDetailVO update(Long id, InboundOrderSaveRequest request) {
        requireWarehouse(request.getWarehouseId());
        validateItems(request);
        InboundOrder order = requireDraft(id);
        BeanUtils.copyProperties(request, order);
        order.setId(id);
        inboundOrderMapper.update(order);
        inboundOrderMapper.deleteItems(id);
        insertItems(id, request);
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireDraft(id);
        inboundOrderMapper.deleteItems(id);
        inboundOrderMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public InboundOrderDetailVO submit(Long id) {
        InboundOrder order = inboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        if (inboundOrderMapper.selectItems(id).isEmpty()) {
            throw BizException.statusInvalid("入库到货单没有明细，不能提交: " + order.getOrderNo());
        }
        StatusTransitions.requireInbound(order.getStatus(), STATUS_PENDING, order.getOrderNo());
        inboundOrderMapper.updateStatus(id, STATUS_PENDING);
        return getById(id);
    }

    @Override
    @Transactional
    public InboundOrderDetailVO cancel(Long id) {
        InboundOrder order = inboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        StatusTransitions.requireInbound(order.getStatus(), 5, order.getOrderNo());
        inboundOrderMapper.updateStatus(id, 5);
        return getById(id);
    }

    @Override
    @Transactional
    public InboundReceiptVO receipt(Long id, ReceiptRequest request) {
        InboundOrder order = inboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        // 状态机：只有待收货 / 部分收货可以继续收货，已完成与已取消一律拒绝
        if (order.getStatus() != STATUS_PENDING && order.getStatus() != STATUS_PARTIAL) {
            throw BizException.statusInvalid(
                    "入库到货单 " + order.getOrderNo() + " 当前状态 " + order.getStatus() + " 不允许收货");
        }
        Location location = requireLocation(request.getLocationId(), order.getWarehouseId(), 2, "收货暂存位");

        InboundReceipt receipt = new InboundReceipt();
        receipt.setReceiptNo(docNoGenerator.next(DocNoGenerator.RECEIPT));
        receipt.setInboundId(id);
        receipt.setLocationId(request.getLocationId());
        receipt.setOperator(request.getOperator());
        receipt.setRemark(request.getRemark());
        inboundOrderMapper.insertReceipt(receipt);

        BizRef biz = BizRef.of(StockService.BIZ_INBOUND, order.getOrderNo());
        for (ReceiptRequest.Item item : request.getItems()) {
            receiveOne(order, receipt, item, request.getOperator());
        }

        // 明细全部收满 -> 已完成；否则部分收货
        int unfinished = inboundOrderMapper.countUnfinishedItems(id);
        if (unfinished == 0) {
            inboundOrderMapper.markFinished(id, OffsetDateTime.now());
        } else {
            inboundOrderMapper.updateStatus(id, STATUS_PARTIAL);
        }
        return toReceiptVO(receipt, biz);
    }

    @Override
    public List<InboundReceiptVO> listReceipts(Long id) {
        if (inboundOrderMapper.selectById(id) == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        return inboundOrderMapper.selectReceipts(id).stream()
                .map(r -> toReceiptVO(r, null))
                .toList();
    }

    // ---------------------------------------------------------------- 内部

    /** 单条收货明细：超收校验 -> 落库存 -> 写流水 -> 生成上架任务 -> 累加已收数量 */
    private void receiveOne(InboundOrder order, InboundReceipt receipt, ReceiptRequest.Item item, String operator) {
        InboundItem inboundItem = inboundOrderMapper.selectItemForUpdate(item.getInboundItemId());
        if (inboundItem == null || !inboundItem.getInboundId().equals(order.getId())) {
            throw BizException.relatedInvalid("收货明细不属于该到货单: " + item.getInboundItemId());
        }
        Sku sku = requireSku(inboundItem.getSkuId());
        boolean serialManaged = sku.getSerialManaged() != null && sku.getSerialManaged() == 1;

        // 规则 1：累加已收数量不得超过应收数量，超收 40903
        BigDecimal already = inboundItem.getReceivedQty() == null ? BigDecimal.ZERO : inboundItem.getReceivedQty();
        BigDecimal after = already.add(item.getQty());
        if (after.compareTo(inboundItem.getExpectedQty()) > 0) {
            throw BizException.qtyNotEnough(sku.getCode() + " 应收 " + inboundItem.getExpectedQty()
                    + "，已收 " + already + "，本次收 " + item.getQty()
                    + "，超收 " + after.subtract(inboundItem.getExpectedQty()));
        }

        // 规则 3：开序列号管理时必须逐条带序列号，且台账中不得已存在
        if (serialManaged) {
            if (item.getSerialNo() == null || item.getSerialNo().isBlank()) {
                throw BizException.paramInvalid("SKU " + sku.getCode() + " 开启序列号管理，收货必须逐条填写序列号");
            }
            if (serialMapper.selectBySerialNo(item.getSerialNo()) != null) {
                throw BizException.serialInvalid("序列号已存在: " + item.getSerialNo());
            }
        } else if (item.getSerialNo() != null && !item.getSerialNo().isBlank()) {
            throw BizException.paramInvalid("SKU " + sku.getCode() + " 未开启序列号管理，不能填序列号");
        }

        // 规则 2：在库量落到收货位，同事务写流水 movementType = 1
        var inventory = stockService.lockOrCreate(order.getWarehouseId(), receipt.getLocationId(),
                inboundItem.getSkuId(), item.getBatchNo(), item.getSerialNo(),
                item.getProducedDate(), item.getExpireDate());
        stockService.changeOnHand(inventory, item.getQty(), MovementType.INBOUND_RECEIPT,
                BizRef.of(StockService.BIZ_INBOUND, order.getOrderNo()), operator, requestRemark(order));

        InboundReceiptItem receiptItem = new InboundReceiptItem();
        receiptItem.setReceiptId(receipt.getId());
        receiptItem.setInboundItemId(inboundItem.getId());
        receiptItem.setSkuId(inboundItem.getSkuId());
        receiptItem.setBatchNo(item.getBatchNo());
        receiptItem.setSerialNo(item.getSerialNo());
        receiptItem.setProducedDate(item.getProducedDate());
        receiptItem.setExpireDate(item.getExpireDate());
        receiptItem.setQty(item.getQty());
        inboundOrderMapper.insertReceiptItem(receiptItem);

        if (serialManaged) {
            Serial serial = new Serial();
            serial.setSerialNo(item.getSerialNo());
            serial.setSkuId(inboundItem.getSkuId());
            serial.setWarehouseId(order.getWarehouseId());
            serial.setLocationId(receipt.getLocationId());
            serial.setStatus(1);
            serialMapper.insert(serial);
        }

        // 规则 4：自动生成上架任务，源库位 = 收货位
        var task = new com.wms.entity.PutawayTask();
        task.setTaskNo(docNoGenerator.next(DocNoGenerator.PUTAWAY_TASK));
        task.setReceiptItemId(receiptItem.getId());
        task.setInboundId(order.getId());
        task.setSkuId(inboundItem.getSkuId());
        task.setFromLocationId(receipt.getLocationId());
        task.setBatchNo(item.getBatchNo());
        task.setSerialNo(item.getSerialNo());
        // **效期随批次走**：上架任务要带着批次的效期，
        // 否则执行上架时库存行拿不到效期（货到存储位效期就没了）。
        task.setProducedDate(item.getProducedDate());
        task.setExpireDate(item.getExpireDate());
        task.setQty(item.getQty());
        task.setStatus(1);
        putawayTaskMapper.insert(task);

        inboundOrderMapper.addReceivedQty(inboundItem.getId(), item.getQty());
    }

    private String requestRemark(InboundOrder order) {
        return order.getRemark();
    }

    private InboundReceiptVO toReceiptVO(InboundReceipt receipt, BizRef biz) {
        InboundReceiptVO vo = new InboundReceiptVO();
        BeanUtils.copyProperties(receipt, vo);
        if (biz != null) {
            vo.setRemark(receipt.getRemark());
        }
        List<InboundReceiptItemVO> items = inboundOrderMapper.selectReceiptItems(receipt.getId());
        vo.setItems(items);
        Location location = locationMapper.selectById(receipt.getLocationId());
        vo.setLocationCode(location == null ? null : location.getCode());
        return vo;
    }

    private void insertItems(Long inboundId, InboundOrderSaveRequest request) {
        for (InboundOrderSaveRequest.Item item : request.getItems()) {
            requireSku(item.getSkuId());
            InboundItem entity = new InboundItem();
            entity.setInboundId(inboundId);
            entity.setSkuId(item.getSkuId());
            entity.setExpectedQty(item.getExpectedQty());
            entity.setReceivedQty(BigDecimal.ZERO);
            inboundOrderMapper.insertItem(entity);
        }
    }

    /** items 至少 1 条且同一 SKU 只能出现一次，重复返回 40001 */
    private void validateItems(InboundOrderSaveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BizException.paramInvalid("明细至少 1 条");
        }
        Set<Long> seen = new HashSet<>();
        for (InboundOrderSaveRequest.Item item : request.getItems()) {
            if (!seen.add(item.getSkuId())) {
                throw BizException.paramInvalid("同一 SKU 只能出现一次: " + item.getSkuId());
            }
        }
    }

    private InboundOrder requireDraft(Long id) {
        InboundOrder order = inboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("入库到货单不存在: " + id);
        }
        if (order.getStatus() != STATUS_DRAFT) {
            throw BizException.orderStatusNotAllowed(
                    "只有草稿状态可以修改/删除，当前状态: " + order.getStatus());
        }
        return order;
    }

    private void requireWarehouse(Long warehouseId) {
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + warehouseId);
        }
    }

    private Sku requireSku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw BizException.relatedInvalid("商品不存在: " + skuId);
        }
        return sku;
    }

    /** 库位必须属于该仓库、类型匹配且启用 */
    private Location requireLocation(Long locationId, Long warehouseId, int locationType, String typeName) {
        Location location = locationMapper.selectById(locationId);
        if (location == null) {
            throw BizException.relatedInvalid("库位不存在: " + locationId);
        }
        if (!location.getWarehouseId().equals(warehouseId)) {
            throw BizException.relatedInvalid("库位不属于该仓库: " + location.getCode());
        }
        if (location.getLocationType() == null || location.getLocationType() != locationType) {
            throw BizException.relatedInvalid(
                    "库位 " + location.getCode() + " 不是" + typeName + "（locationType 必须为 " + locationType + "）");
        }
        if (location.getStatus() == null || location.getStatus() != 1) {
            throw BizException.relatedInvalid("库位已停用: " + location.getCode());
        }
        return location;
    }
}
