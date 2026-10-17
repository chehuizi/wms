package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 入库到货单 wms_inbound_order。status: 1=草稿 2=待收货 3=部分收货 4=已完成 5=已取消。
 */
@Data
public class InboundOrder {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String supplierCode;
    private String supplierName;
    private Integer status;
    private OffsetDateTime expectedAt;
    private OffsetDateTime finishedAt;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
