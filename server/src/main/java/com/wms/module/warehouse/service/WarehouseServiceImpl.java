package com.wms.module.warehouse.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.WarehouseQuery;
import com.wms.dto.WarehouseSaveRequest;
import com.wms.entity.Warehouse;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.warehouse.WarehouseService;
import com.wms.vo.WarehouseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper, LocationMapper locationMapper) {
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
    }

    @Override
    public PageResult<WarehouseVO> page(WarehouseQuery query) {
        long total = warehouseMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<WarehouseVO> list = warehouseMapper.selectPage(query).stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public WarehouseVO getById(Long id) {
        return toVO(requireExists(id));
    }

    @Override
    public WarehouseVO create(WarehouseSaveRequest request) {
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(request, warehouse);
        warehouse.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        try {
            warehouseMapper.insert(warehouse);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("仓库编码已存在: " + request.getCode());
        }
        return getById(warehouse.getId());
    }

    @Override
    @Transactional
    public WarehouseVO update(Long id, WarehouseSaveRequest request) {
        Warehouse existing = requireExists(id);
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(request, warehouse);
        warehouse.setId(existing.getId());
        warehouse.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        try {
            warehouseMapper.update(warehouse);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("仓库编码已存在: " + request.getCode());
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        assertNoActiveLocations(id);
        warehouseMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 逐个校验存在性与级联保护，保证错误码精确
        for (Long id : ids) {
            requireExists(id);
            assertNoActiveLocations(id);
        }
        warehouseMapper.softDeleteByIds(ids);
    }

    /** 仓库下仍有未删除库位时拒绝删除（契约 40902） */
    private void assertNoActiveLocations(Long warehouseId) {
        long count = locationMapper.countActiveByWarehouseId(warehouseId);
        if (count > 0) {
            throw BizException.hasRelation("该仓库下仍有 " + count + " 个库位，无法删除");
        }
    }

    private Warehouse requireExists(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw BizException.notFound("仓库不存在: " + id);
        }
        return warehouse;
    }

    private WarehouseVO toVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        BeanUtils.copyProperties(warehouse, vo);
        return vo;
    }
}
