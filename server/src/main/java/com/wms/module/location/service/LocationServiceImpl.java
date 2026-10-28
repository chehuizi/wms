package com.wms.module.location.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.LocationQuery;
import com.wms.dto.LocationSaveRequest;
import com.wms.entity.Location;
import com.wms.entity.Zone;
import com.wms.entity.Warehouse;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.mapper.ZoneMapper;
import com.wms.module.location.LocationService;
import com.wms.vo.LocationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationMapper locationMapper;
    private final WarehouseMapper warehouseMapper;
    private final ZoneMapper zoneMapper;

    public LocationServiceImpl(LocationMapper locationMapper,
                                WarehouseMapper warehouseMapper,
                                ZoneMapper zoneMapper) {
        this.locationMapper = locationMapper;
        this.warehouseMapper = warehouseMapper;
        this.zoneMapper = zoneMapper;
    }

    @Override
    public PageResult<LocationVO> page(LocationQuery query) {
        long total = locationMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<LocationVO> list = locationMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public LocationVO getById(Long id) {
        LocationVO vo = locationMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("库位不存在: " + id);
        }
        return vo;
    }

    @Override
    public LocationVO create(LocationSaveRequest request) {
        requireActiveWarehouse(request.getWarehouseId());
        Location location = new Location();
        BeanUtils.copyProperties(request, location);
        applyDefaults(location);
        try {
            locationMapper.insert(location);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode(
                    "同一仓库下库位编码已存在: " + request.getCode());
        }
        return getById(location.getId());
    }

    @Override
    @Transactional
    public LocationVO update(Long id, LocationSaveRequest request) {
        requireExists(id);
        requireActiveWarehouse(request.getWarehouseId());
        Location location = new Location();
        BeanUtils.copyProperties(request, location);
        location.setId(id);
        applyDefaults(location);
        try {
            locationMapper.update(location);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode(
                    "同一仓库下库位编码已存在: " + request.getCode());
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        locationMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            requireExists(id);
        }
        locationMapper.softDeleteByIds(ids);
    }

    /**
     * 库位必须挂在存在且启用的仓库上（契约 40003）。
     */
    private void requireActiveWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseId == null ? null : warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw BizException.relatedInvalid("仓库不存在: " + warehouseId);
        }
        if (warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw BizException.relatedInvalid("仓库已停用: " + warehouse.getName());
        }
    }

    /**
     * 库位所属库区必须存在，且库区必须隶属于指定仓库。
     * Zone 通过外键字段 zoneId 与库位关联，不复制库区实体。
     */
    private Zone requireZone(Long zoneId, Long warehouseId) {
        if (zoneId == null) {
            return null;
        }
        Zone zone = zoneMapper.selectById(zoneId);
        if (zone == null) {
            throw BizException.relatedInvalid("库区不存在: " + zoneId);
        }
        if (!zone.getWarehouseId().equals(warehouseId)) {
            throw BizException.relatedInvalid("库区不属于指定仓库: " + zoneId);
        }
        return zone;
    }

    private void requireExists(Long id) {
        Location location = locationMapper.selectById(id);
        if (location == null) {
            throw BizException.notFound("库位不存在: " + id);
        }
    }

    private void applyDefaults(Location location) {
        if (location.getLocationType() == null) {
            location.setLocationType(1);
        }
        if (location.getAllowNegative() == null) {
            location.setAllowNegative(0);
        }
        if (location.getStatus() == null) {
            location.setStatus(1);
        }
    }
}
