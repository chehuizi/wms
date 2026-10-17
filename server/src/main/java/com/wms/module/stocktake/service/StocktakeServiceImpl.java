package com.wms.module.stocktake.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.StocktakeApproveRequest;
import com.wms.dto.StocktakeCountRequest;
import com.wms.dto.StocktakeCreateRequest;
import com.wms.dto.StocktakeOrderQuery;
import com.wms.entity.Inventory;
import com.wms.entity.StocktakeAdjust;
import com.wms.entity.StocktakeItem;
import com.wms.entity.StocktakeOrder;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.StocktakeMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.common.StatusTransitions;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.module.stocktake.StocktakeService;
import com.wms.vo.StocktakeOrderDetailVO;
import com.wms.vo.StocktakeOrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点实现，见契约第 17 节。
 * <p>
 * 调账全部走 {@link StockService}，盘盈写 3 号流水、盘亏写 4 号流水，与在库量同事务。
 */
@Service
public class StocktakeServiceImpl implements StocktakeService {

    /** 状态：1=草稿 2=进行中 3=待审批 4=已完成 5=已取消 */
    private static final int STATUS_DRAFT = 1;
    private static final int STATUS_COUNTING = 2;
    private static final int STATUS_PENDING_APPROVE = 3;
    private static final int STATUS_DONE = 4;

    /** 明细状态：1=待盘 2=已盘 3=待审批 4=已调账 */
    private static final int ITEM_COUNTED = 2;
    private static final int ITEM_ADJUSTED = 4;

    /** 全盘 */
    private static final int TYPE_FULL = 1;

    private final StocktakeMapper stocktakeMapper;
    private final InventoryMapper inventoryMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockService stockService;
    private final DocNoGenerator docNoGenerator;

    public StocktakeServiceImpl(StocktakeMapper stocktakeMapper,
                                InventoryMapper inventoryMapper,
                                WarehouseMapper warehouseMapper,
                                StockService stockService,
                                DocNoGenerator docNoGenerator) {
        this.stocktakeMapper = stocktakeMapper;
        this.inventoryMapper = inventoryMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockService = stockService;
        this.docNoGenerator = docNoGenerator;
    }

    @Override
    public PageResult<StocktakeOrderVO> page(StocktakeOrderQuery query) {
        long total = stocktakeMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(stocktakeMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public StocktakeOrderDetailVO getById(Long id) {
        StocktakeOrderVO vo = stocktakeMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("盘点单不存在: " + id);
        }
        StocktakeOrderDetailVO detail = new StocktakeOrderDetailVO();
        BeanUtils.copyProperties(vo, detail);
        detail.setItems(stocktakeMapper.selectItems(id));
        detail.setAdjusts(stocktakeMapper.selectAdjusts(id));
        return detail;
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO create(StocktakeCreateRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + request.getWarehouseId());
        }
        Integer type = request.getStocktakeType();
        if (type == null) {
            throw BizException.paramInvalid("盘点类型必填");
        }
        // stocktakeType != 1 时 scopeZone 必填
        if (type != TYPE_FULL && (request.getScopeZone() == null || request.getScopeZone().isBlank())) {
            throw BizException.paramInvalid("循环盘/抽盘必须指定盘点库区 scopeZone");
        }

        StocktakeOrder order = new StocktakeOrder();
        order.setOrderNo(docNoGenerator.next(DocNoGenerator.STOCKTAKE_ORDER));
        order.setWarehouseId(request.getWarehouseId());
        order.setStocktakeType(type);
        order.setStatus(STATUS_DRAFT);
        order.setScopeZone(request.getScopeZone());
        order.setTotalLocations(0);
        order.setCountedLocations(0);
        order.setRemark(request.getRemark());
        order.setDeleted(0);
        stocktakeMapper.insert(order);

        // 对范围内每条在库量 > 0 的库存行生成明细，bookQty 为快照
        stocktakeMapper.insertItemsFromInventory(order.getId(), order.getWarehouseId(), order.getScopeZone());
        stocktakeMapper.setTotalLocations(order.getId(), countLocations(order.getId()));
        return getById(order.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireStatus(id, STATUS_DRAFT);
        stocktakeMapper.deleteItems(id);
        stocktakeMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO start(Long id) {
        StocktakeOrder order = requireStatus(id, STATUS_DRAFT);
        StatusTransitions.requireStocktake(order.getStatus(), STATUS_COUNTING, order.getOrderNo());
        stocktakeMapper.markStarted(id);
        return getById(id);
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO count(Long id, StocktakeCountRequest request) {
        StocktakeOrder order = stocktakeMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在: " + id);
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_COUNTING) {
            throw BizException.orderStatusNotAllowed(
                    "只有进行中的盘点单可以录入实盘，当前状态: " + order.getStatus());
        }
        for (StocktakeCountRequest.Item item : request.getItems()) {
            StocktakeItem entity = stocktakeMapper.selectItemForUpdate(item.getItemId());
            if (entity == null || !entity.getStocktakeId().equals(id)) {
                throw BizException.relatedInvalid("盘点明细不属于该盘点单: " + item.getItemId());
            }
            if (item.getCountedQty() == null || item.getCountedQty().signum() < 0) {
                throw BizException.paramInvalid("实盘数量必填且不能为负: " + item.getItemId());
            }
            BigDecimal bookQty = entity.getBookQty() == null ? BigDecimal.ZERO : entity.getBookQty();
            BigDecimal diff = item.getCountedQty().subtract(bookQty);
            // 重复提交视为覆盖
            stocktakeMapper.updateCounted(entity.getId(), item.getCountedQty(), diff,
                    request.getOperator(), item.getRemark());
        }
        stocktakeMapper.markAllItemsStatus(id, ITEM_COUNTED);
        stocktakeMapper.refreshCountedLocations(id);
        return getById(id);
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO finish(Long id) {
        StocktakeOrder order = requireStatus(id, STATUS_COUNTING);
        int uncounted = stocktakeMapper.countUncountedItems(id);
        if (uncounted > 0) {
            throw BizException.stocktakeIncomplete(
                    "还有 " + uncounted + " 条明细未录入实盘，不能结束盘点: " + order.getOrderNo());
        }
        stocktakeMapper.markFinished(id);
        int diffItems = stocktakeMapper.countDiffItems(id);
        // 无差异直接完成，有差异转待审批
        stocktakeMapper.updateStatus(id, diffItems == 0 ? STATUS_DONE : STATUS_PENDING_APPROVE);
        return getById(id);
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO approve(Long id, StocktakeApproveRequest request) {
        StocktakeOrder order = requireStatus(id, STATUS_PENDING_APPROVE);
        BizRef biz = BizRef.of(StockService.BIZ_STOCKTAKE, order.getOrderNo());

        for (StocktakeItem item : stocktakeMapper.selectItemEntities(id)) {
            BigDecimal diff = item.getDiffQty();
            if (diff != null && diff.signum() != 0) {
                // 差异调整：盘盈写 3 号流水，盘亏写 4 号流水
                Inventory inventory = item.getInventoryId() == null
                        ? null
                        : inventoryMapper.selectByIdForUpdate(item.getInventoryId());
                if (inventory == null) {
                    throw BizException.notFound("盘点明细关联的库存行不存在: " + item.getId());
                }
                int movementType = diff.signum() > 0 ? MovementType.STOCK_GAIN : MovementType.STOCK_LOSS;
                stockService.changeOnHand(inventory, diff, movementType, biz, request.getApprover(),
                        request.getRemark());

                StocktakeAdjust adjust = new StocktakeAdjust();
                adjust.setStocktakeId(id);
                adjust.setStocktakeItemId(item.getId());
                adjust.setInventoryId(inventory.getId());
                adjust.setSkuId(item.getSkuId());
                adjust.setLocationId(item.getLocationId());
                adjust.setDiffQty(diff);
                adjust.setApprovedBy(request.getApprover());
                adjust.setRemark(request.getRemark());
                stocktakeMapper.insertAdjust(adjust);
            }
            // diffQty = 0 的明细不写流水，只置为已调账
            stocktakeMapper.markItemStatus(item.getId(), ITEM_ADJUSTED);
        }
        stocktakeMapper.markApproved(id, request.getApprover());
        return getById(id);
    }

    @Override
    @Transactional
    public StocktakeOrderDetailVO cancel(Long id) {
        StocktakeOrder order = stocktakeMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在: " + id);
        }
        if (order.getStatus() != STATUS_DRAFT && order.getStatus() != STATUS_COUNTING) {
            throw BizException.orderStatusNotAllowed(
                    "只有草稿或进行中的盘点单可以取消，当前状态: " + order.getStatus());
        }
        // 取消不写任何流水
        stocktakeMapper.markCancelled(id);
        return getById(id);
    }

    private int countLocations(Long stocktakeId) {
        return stocktakeMapper.selectItems(stocktakeId).stream()
                .map(item -> item.getLocationId())
                .distinct()
                .toList()
                .size();
    }

    private StocktakeOrder requireStatus(Long id, int required) {
        StocktakeOrder order = stocktakeMapper.selectForUpdate(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在: " + id);
        }
        if (order.getStatus() == null || order.getStatus() != required) {
            throw BizException.orderStatusNotAllowed(
                    "当前状态 " + order.getStatus() + " 不允许该操作（要求状态 " + required + "）: " + order.getOrderNo());
        }
        return order;
    }
}
