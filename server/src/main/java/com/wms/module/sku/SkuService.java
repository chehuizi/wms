package com.wms.module.sku;

import com.wms.common.PageResult;
import com.wms.dto.SkuQuery;
import com.wms.dto.SkuSaveRequest;
import com.wms.vo.SkuVO;

import java.util.List;

public interface SkuService {

    PageResult<SkuVO> page(SkuQuery query);

    SkuVO getById(Long id);

    SkuVO create(SkuSaveRequest request);

    SkuVO update(Long id, SkuSaveRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    List<String> categories();
}
