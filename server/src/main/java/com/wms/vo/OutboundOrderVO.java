package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 出库订单响应对象，见契约第 16.1 节。
 */
@Data
public class OutboundOrderVO {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private Long customerId;
    private String customerName;
    private Integer status;
    private Integer priority;
    private OffsetDateTime expectedShipAt;
    private OffsetDateTime shippedAt;
    private String remark;
    private Integer itemCount;
    private BigDecimal orderedQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
