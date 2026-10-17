package com.wms.module.stock.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.InventoryAdjustRequest;
import com.wms.dto.InventoryQuery;
import com.wms.dto.MovementQuery;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.StockMovementMapper;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.InventoryService;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.vo.InventoryVO;
import com.wms.vo.StockMovementVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存台账查询与手工调整实现，见契约第 13 节。
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper movementMapper;
    private final LocationMapper locationMapper;
    private final StockService stockService;

    public InventoryServiceImpl(InventoryMapper inventoryMapper,
                                StockMovementMapper movementMapper,
                                LocationMapper locationMapper,
                                StockService stockService) {
        this.inventoryMapper = inventoryMapper;
        this.movementMapper = movementMapper;
        this.locationMapper = locationMapper;
        this.stockService = stockService;
    }

    @Override
    public PageResult<InventoryVO> page(InventoryQuery query) {
        long total = inventoryMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<InventoryVO> list = inventoryMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public InventoryVO getById(Long id) {
        InventoryVO vo = inventoryMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("库存记录不存在: " + id);
        }
        return vo;
    }

    @Override
    public PageResult<StockMovementVO> pageMovements(MovementQuery query) {
        long total = movementMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<StockMovementVO> list = movementMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    @Transactional
    public InventoryVO adjust(InventoryAdjustRequest request) {
        if (request.getQtyChange() == null || request.getQtyChange().signum() == 0) {
            throw BizException.paramInvalid("调整数量不能为 0");
        }
        Inventory locked = request.getInventoryId() != null
                ? lockByInventoryId(request.getInventoryId())
                : createTargetRow(request);
        stockService.changeOnHand(locked, request.getQtyChange(), MovementType.MANUAL_ADJUST,
                BizRef.of(StockService.BIZ_MANUAL, null), request.getOperator(), request.getReason());
        return getById(locked.getId());
    }

    /** 已存在则按原唯一键重新加锁，确保并发下不会被别的请求抢先改掉 */
    private Inventory lockByInventoryId(Long inventoryId) {
        Inventory existing = inventoryMapper.selectById(inventoryId);
        if (existing == null) {
            throw BizException.notFound("库存记录不存在: " + inventoryId);
        }
        return stockService.lockOrCreate(existing.getWarehouseId(), existing.getLocationId(),
                existing.getSkuId(), existing.getBatchNo(), existing.getSerialNo(),
                existing.getProducedDate(), existing.getExpireDate());
    }

    /** 未传 inventoryId 时按 仓库 + 库位 + SKU（+批次+序列号）新建一条库存行 */
    private Inventory createTargetRow(InventoryAdjustRequest request) {
        if (request.getWarehouseId() == null || request.getLocationId() == null
                || request.getSkuId() == null) {
            throw BizException.paramInvalid("新建库存行时 warehouseId / locationId / skuId 必填");
        }
        Location location = locationMapper.selectById(request.getLocationId());
        if (location == null) {
            throw BizException.relatedInvalid("库位不存在: " + request.getLocationId());
        }
        if (!location.getWarehouseId().equals(request.getWarehouseId())) {
            throw BizException.relatedInvalid("库位不属于指定仓库: " + request.getLocationId());
        }
        return stockService.lockOrCreate(request.getWarehouseId(), request.getLocationId(),
                request.getSkuId(), request.getBatchNo(), request.getSerialNo(), null, null);
    }
}
