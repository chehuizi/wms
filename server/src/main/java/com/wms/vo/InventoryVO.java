package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 库存台账响应对象，见契约第 13 节。availableQty 为计算字段，由后端算好返回。
 */
@Data
public class InventoryVO {

    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private String zone;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String category;
    private String unit;
    private String batchNo;
    private String serialNo;
    private LocalDate producedDate;
    private LocalDate expireDate;
    private BigDecimal onHandQty;
    private BigDecimal lockedQty;
    /** 计算字段：onHandQty - lockedQty */
    private BigDecimal availableQty;
    private Integer status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
