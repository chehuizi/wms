package com.wms.vo;

import lombok.Data;

import java.util.List;

/**
 * 出库订单详情 = 单头 + 明细 + 分配记录，见契约第 16.1 节。
 */
@Data
public class OutboundOrderDetailVO extends OutboundOrderVO {

    private List<OutboundItemVO> items;
    private List<AllocationVO> allocations;
}
