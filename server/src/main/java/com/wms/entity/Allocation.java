package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库存分配记录 wms_allocation。status: 1=已锁定 2=拣货中 3=已拣货 4=已发货 5=已释放。
 */
@Data
public class Allocation {

    private Long id;
    private Long outboundId;
    private Long outboundItemId;
    private Long inventoryId;
    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private String batchNo;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
    private Integer status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
