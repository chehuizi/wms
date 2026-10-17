package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 入库收货记录 wms_inbound_receipt，一张到货单可分多次收货。
 */
@Data
public class InboundReceipt {

    private Long id;
    private String receiptNo;
    private Long inboundId;
    private Long locationId;
    private OffsetDateTime receivedAt;
    private String operator;
    private String remark;
    private OffsetDateTime createdAt;
}
