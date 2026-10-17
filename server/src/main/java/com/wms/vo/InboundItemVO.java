package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 到货单明细响应对象。
 */
@Data
public class InboundItemVO {

    private Long id;
    private Long inboundId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String unit;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
}
