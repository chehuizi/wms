package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 出库订单 wms_outbound_order。
 * status: 1=草稿 2=待分配 3=已分配 4=拣货中 5=待发货 6=已发货 7=已取消。
 */
@Data
public class OutboundOrder {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private Long customerId;
    private Integer status;
    private Integer priority;
    private OffsetDateTime expectedShipAt;
    private OffsetDateTime shippedAt;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
