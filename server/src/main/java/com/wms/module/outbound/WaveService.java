package com.wms.module.outbound;

import com.wms.common.PageResult;
import com.wms.dto.WaveCreateRequest;
import com.wms.dto.WaveOrdersRequest;
import com.wms.dto.WaveQuery;
import com.wms.vo.WaveVO;

/**
 * 拣货波次，见契约第 16.3 节。
 */
public interface WaveService {

    PageResult<WaveVO> page(WaveQuery query);

    WaveVO getById(Long id);

    WaveVO create(WaveCreateRequest request);

    WaveVO addOrders(Long id, WaveOrdersRequest request);

    /** 释放波次：按库位编码升序生成拣货任务，订单转拣货中 */
    WaveVO release(Long id);
}
