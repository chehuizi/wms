package com.wms.module.inbound;

import com.wms.common.PageResult;
import com.wms.dto.PutawayTaskQuery;
import com.wms.dto.PutawayExecuteRequest;
import com.wms.vo.PutawayTaskVO;

/**
 * 上架任务，见契约第 15.2 节。执行上架 = 一次移库，产生 5/6 两条流水。
 */
public interface PutawayService {

    PageResult<PutawayTaskVO> page(PutawayTaskQuery query);

    PutawayTaskVO execute(Long id, PutawayExecuteRequest request);
}
