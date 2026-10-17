package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 收货明细 wms_inbound_receipt_item。
 */
@Data
public class InboundReceiptItem {

    private Long id;
    private Long receiptId;
    private Long inboundItemId;
    private Long skuId;
    private String batchNo;
    private String serialNo;
    private LocalDate producedDate;
    private LocalDate expireDate;
    private BigDecimal qty;
    private OffsetDateTime createdAt;
}
