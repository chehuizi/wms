package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库订单明细响应对象。
 */
@Data
public class OutboundItemVO {

    private Long id;
    private Long outboundId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String unit;
    private BigDecimal orderedQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
}
