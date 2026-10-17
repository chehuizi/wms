package com.wms.module.warehouse;

import com.wms.common.PageResult;
import com.wms.dto.WarehouseQuery;
import com.wms.dto.WarehouseSaveRequest;
import com.wms.vo.WarehouseVO;

import java.util.List;

public interface WarehouseService {

    PageResult<WarehouseVO> page(WarehouseQuery query);

    WarehouseVO getById(Long id);

    WarehouseVO create(WarehouseSaveRequest request);

    WarehouseVO update(Long id, WarehouseSaveRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);
}
