package com.wms.vo;

import lombok.Data;

import java.util.List;

/**
 * 入库到货单详情 = 单头 + 明细，见契约第 15.1 节。
 */
@Data
public class InboundOrderDetailVO extends InboundOrderVO {

    private List<InboundItemVO> items;
}
