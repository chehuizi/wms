package com.wms.module.zone;

import com.wms.common.PageResult;
import com.wms.dto.ZoneQuery;
import com.wms.dto.ZoneSaveRequest;
import com.wms.vo.ZoneVO;
import com.wms.entity.Location;

import java.util.List;

public interface ZoneService {

    PageResult<ZoneVO> page(ZoneQuery query);

    ZoneVO getById(Long id);

    ZoneVO create(ZoneSaveRequest request);

    ZoneVO update(Long id, ZoneSaveRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    /** 查询指定库区下未删除的库位 */
    List<Location> listLocations(Long zoneId);
}
