package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 到货单明细 wms_inbound_item。
 */
@Data
public class InboundItem {

    private Long id;
    private Long inboundId;
    private Long skuId;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
