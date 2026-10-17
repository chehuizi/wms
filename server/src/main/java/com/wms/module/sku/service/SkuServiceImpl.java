package com.wms.module.sku.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.SkuQuery;
import com.wms.dto.SkuSaveRequest;
import com.wms.entity.Sku;
import com.wms.mapper.SkuMapper;
import com.wms.module.sku.SkuService;
import com.wms.vo.SkuVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {

    private final SkuMapper skuMapper;

    public SkuServiceImpl(SkuMapper skuMapper) {
        this.skuMapper = skuMapper;
    }

    @Override
    public PageResult<SkuVO> page(SkuQuery query) {
        long total = skuMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<SkuVO> list = skuMapper.selectPage(query).stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public SkuVO getById(Long id) {
        return toVO(requireExists(id));
    }

    @Override
    public List<String> categories() {
        return skuMapper.selectDistinctCategories();
    }

    @Override
    public SkuVO create(SkuSaveRequest request) {
        Sku sku = new Sku();
        BeanUtils.copyProperties(request, sku);
        applyDefaults(sku);
        try {
            skuMapper.insert(sku);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("商品编码已存在: " + request.getCode());
        }
        return getById(sku.getId());
    }

    @Override
    @Transactional
    public SkuVO update(Long id, SkuSaveRequest request) {
        requireExists(id);
        Sku sku = new Sku();
        BeanUtils.copyProperties(request, sku);
        sku.setId(id);
        applyDefaults(sku);
        try {
            skuMapper.update(sku);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("商品编码已存在: " + request.getCode());
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        skuMapper.softDeleteById(id);
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
        skuMapper.softDeleteByIds(ids);
    }

    /**
     * 业务约定：shelfLifeDays 为 0 视同无保质期，落库为 NULL。
     */
    private void applyDefaults(Sku sku) {
        if (sku.getBatchManaged() == null) {
            sku.setBatchManaged(0);
        }
        if (sku.getSerialManaged() == null) {
            sku.setSerialManaged(0);
        }
        if (sku.getStatus() == null) {
            sku.setStatus(1);
        }
        if (sku.getShelfLifeDays() != null && sku.getShelfLifeDays() == 0) {
            sku.setShelfLifeDays(null);
        }
    }

    private Sku requireExists(Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw BizException.notFound("商品不存在: " + id);
        }
        return sku;
    }

    private SkuVO toVO(Sku sku) {
        SkuVO vo = new SkuVO();
        BeanUtils.copyProperties(sku, vo);
        return vo;
    }
}
