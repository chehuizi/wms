package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 发货单 wms_shipment，无状态，发货即终态。
 */
@Data
public class Shipment {

    private Long id;
    private String shipmentNo;
    private Long outboundId;
    private String carrier;
    private String trackingNo;
    private OffsetDateTime shippedAt;
    private String operator;
    private String remark;
    private OffsetDateTime createdAt;
}
