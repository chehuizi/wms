package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 库存台账 wms_inventory。
 * availableQty 不落库，由 onHandQty - lockedQty 实时计算（契约第 10.1 条）。
 */
@Data
public class Inventory {

    private Long id;
    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private String batchNo;
    private String serialNo;
    private LocalDate producedDate;
    private LocalDate expireDate;
    private BigDecimal onHandQty;
    private BigDecimal lockedQty;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
