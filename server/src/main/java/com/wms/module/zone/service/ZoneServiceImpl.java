package com.wms.module.zone.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.ZoneQuery;
import com.wms.dto.ZoneSaveRequest;
import com.wms.entity.Zone;
import com.wms.mapper.ZoneMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.zone.ZoneService;
import com.wms.vo.ZoneVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库区服务。
 */
@Service
public class ZoneServiceImpl implements ZoneService {

    private final ZoneMapper zoneMapper;
    private final WarehouseMapper warehouseMapper;

    public ZoneServiceImpl(ZoneMapper zoneMapper, WarehouseMapper warehouseMapper) {
        this.zoneMapper = zoneMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public PageResult<ZoneVO> page(ZoneQuery query) {
        long total = zoneMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<ZoneVO> list = zoneMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public ZoneVO getById(Long id) {
        ZoneVO vo = zoneMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("库区不存在: " + id);
        }
        return vo;
    }

    @Override
    public ZoneVO create(ZoneSaveRequest request) {
        requireActiveWarehouse(request.getWarehouseId());
        Zone zone = new Zone();
        BeanUtils.copyProperties(request, zone);
        try {
            zoneMapper.insert(zone);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode(
                    "同一仓库下库区编码已存在: " + request.getCode());
        }
        return getById(zone.getId());
    }

    @Override
    @Transactional
    public ZoneVO update(Long id, ZoneSaveRequest request) {
        requireExists(id);
        requireActiveWarehouse(request.getWarehouseId());
        Zone zone = new Zone();
        BeanUtils.copyProperties(request, zone);
        zone.setId(id);
        try {
            zoneMapper.update(zone);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode(
                    "同一仓库下库区编码已存在: " + request.getCode());
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        assertNoActiveLocations(id);
        zoneMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            requireExists(id);
            assertNoActiveLocations(id);
        }
        zoneMapper.softDeleteByIds(ids);
    }

    /** 库区下仍有未删除库位时拒绝删除。 */
    private void assertNoActiveLocations(Long zoneId) {
        long count = zoneMapper.countActiveLocationsByZoneId(zoneId);
        if (count > 0) {
            throw BizException.hasRelation("该库区下仍有 " + count + " 个库位，无法删除");
        }
    }

    private void requireActiveWarehouse(Long warehouseId) {
        if (warehouseId == null || warehouseMapper.selectById(warehouseId) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + warehouseId);
        }
    }

    private Zone requireExists(Long id) {
        Zone zone = zoneMapper.selectById(id);
        if (zone == null) {
            throw BizException.notFound("库区不存在: " + id);
        }
        return zone;
    }
}
