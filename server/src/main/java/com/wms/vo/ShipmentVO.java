package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 发货记录响应对象，见契约第 16.5 节。
 */
@Data
public class ShipmentVO {

    private Long id;
    private String shipmentNo;
    private Long outboundId;
    private String carrier;
    private String trackingNo;
    private OffsetDateTime shippedAt;
    private String operator;
    private String remark;
}
