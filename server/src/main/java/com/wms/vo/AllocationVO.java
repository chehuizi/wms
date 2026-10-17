package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库存分配记录响应对象，见契约第 16.1 节。
 */
@Data
public class AllocationVO {

    private Long id;
    private Long outboundId;
    private Long outboundItemId;
    private Long inventoryId;
    private Long warehouseId;
    private Long locationId;
    private String locationCode;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
    private Integer status;
    private OffsetDateTime createdAt;
}
