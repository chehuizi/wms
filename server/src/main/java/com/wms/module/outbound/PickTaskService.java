package com.wms.module.outbound;

import com.wms.common.PageResult;
import com.wms.dto.PickRequest;
import com.wms.dto.PickTaskQuery;
import com.wms.vo.PickTaskVO;

/**
 * 拣货任务，见契约第 16.4 节。
 */
public interface PickTaskService {

    PageResult<PickTaskVO> page(PickTaskQuery query);

    PickTaskVO pick(Long id, PickRequest request);

    PickTaskVO cancel(Long id);
}
