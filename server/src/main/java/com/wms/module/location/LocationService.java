package com.wms.module.location;

import com.wms.common.PageResult;
import com.wms.dto.LocationQuery;
import com.wms.dto.LocationSaveRequest;
import com.wms.vo.LocationVO;

import java.util.List;

public interface LocationService {

    PageResult<LocationVO> page(LocationQuery query);

    LocationVO getById(Long id);

    LocationVO create(LocationSaveRequest request);

    LocationVO update(Long id, LocationSaveRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);
}
