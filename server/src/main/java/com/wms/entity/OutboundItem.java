package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 出库订单明细 wms_outbound_item。
 */
@Data
public class OutboundItem {

    private Long id;
    private Long outboundId;
    private Long skuId;
    private BigDecimal orderedQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
