package com.wms.module.inbound.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.PutawayExecuteRequest;
import com.wms.dto.PutawayTaskQuery;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.entity.PutawayTask;
import com.wms.entity.Serial;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.PutawayTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.module.inbound.PutawayService;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.StockService;
import com.wms.vo.PutawayTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 上架任务实现，见契约第 15.2 节。
 * 执行上架 = 收货位 → 存储位的移库，同事务产生 5 移库出、6 移库入两条流水。
 */
@Service
public class PutawayServiceImpl implements PutawayService {

    private static final int STATUS_PENDING = 1;

    private final PutawayTaskMapper putawayTaskMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final LocationMapper locationMapper;
    private final SerialMapper serialMapper;
    private final StockService stockService;

    public PutawayServiceImpl(PutawayTaskMapper putawayTaskMapper,
                              InboundOrderMapper inboundOrderMapper,
                              LocationMapper locationMapper,
                              SerialMapper serialMapper,
                              StockService stockService) {
        this.putawayTaskMapper = putawayTaskMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.locationMapper = locationMapper;
        this.serialMapper = serialMapper;
        this.stockService = stockService;
    }

    @Override
    public PageResult<PutawayTaskVO> page(PutawayTaskQuery query) {
        long total = putawayTaskMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(putawayTaskMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    @Transactional
    public PutawayTaskVO execute(Long taskId, PutawayExecuteRequest request) {
        PutawayTask task = putawayTaskMapper.selectForUpdate(taskId);
        if (task == null) {
            throw BizException.notFound("上架任务不存在: " + taskId);
        }
        if (task.getStatus() == null || task.getStatus() != STATUS_PENDING) {
            throw BizException.orderStatusNotAllowed(
                    "只有待上架的任务可以执行，当前状态: " + task.getStatus());
        }
        var inbound = inboundOrderMapper.selectById(task.getInboundId());
        if (inbound == null) {
            throw BizException.relatedInvalid("上架任务关联的到货单不存在: " + task.getInboundId());
        }
        Long warehouseId = inbound.getWarehouseId();

        Location toLocation = requireStorageLocation(request.getToLocationId(), warehouseId);
        if (toLocation.getId().equals(task.getFromLocationId())) {
            throw BizException.paramInvalid("目标库位不能与收货位相同");
        }

        Inventory from = stockService.lockOrCreate(warehouseId, task.getFromLocationId(),
                task.getSkuId(), task.getBatchNo(), task.getSerialNo(), null, null);
        Inventory to = stockService.lockOrCreate(warehouseId, toLocation.getId(),
                task.getSkuId(), task.getBatchNo(), task.getSerialNo(), null, null);

        // 同事务：源位减、目标位加，两条流水由 StockService 统一写
        stockService.moveStock(from.getId(), to.getId(), task.getQty(),
                BizRef.of(StockService.BIZ_INBOUND, inbound.getOrderNo()), request.getOperator(), "上架");

        // 序列号随货移动
        if (task.getSerialNo() != null && !task.getSerialNo().isBlank()) {
            Serial serial = serialMapper.selectBySerialNo(task.getSerialNo());
            if (serial != null) {
                serialMapper.updateLocation(serial.getId(), warehouseId, toLocation.getId(), serial.getStatus());
            }
        }

        putawayTaskMapper.markCompleted(taskId, toLocation.getId(), request.getOperator());

        PutawayTaskVO vo = putawayTaskMapper.selectVOById(taskId);
        if (vo == null) {
            throw BizException.notFound("上架任务不存在: " + taskId);
        }
        return vo;
    }

    private Location requireStorageLocation(Long locationId, Long warehouseId) {
        Location location = locationMapper.selectById(locationId);
        if (location == null) {
            throw BizException.relatedInvalid("库位不存在: " + locationId);
        }
        if (!location.getWarehouseId().equals(warehouseId)) {
            throw BizException.relatedInvalid("库位不属于该仓库: " + location.getCode());
        }
        if (location.getLocationType() == null || location.getLocationType() != 1) {
            throw BizException.relatedInvalid(
                    "库位 " + location.getCode() + " 不是存储位（locationType 必须为 1）");
        }
        if (location.getStatus() == null || location.getStatus() != 1) {
            throw BizException.relatedInvalid("库位已停用: " + location.getCode());
        }
        return location;
    }
}
