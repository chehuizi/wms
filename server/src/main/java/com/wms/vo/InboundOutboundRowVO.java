package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出入库统计行，见契约第 18.3 节。
 */
@Data
public class InboundOutboundRowVO {

    private String period;
    private BigDecimal inboundQty;
    private Long inboundOrders;
    private BigDecimal outboundQty;
    private Long outboundOrders;
}
