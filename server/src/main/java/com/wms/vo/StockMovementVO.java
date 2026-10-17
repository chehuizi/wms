package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库存流水响应对象，见契约第 13 节。
 */
@Data
public class StockMovementVO {

    private Long id;
    private String movementNo;
    private Integer movementType;
    private String movementTypeName;
    private String bizType;
    private String bizNo;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String serialNo;
    private BigDecimal qtyChange;
    private BigDecimal lockedChange;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private String operator;
    private String remark;
    private OffsetDateTime createdAt;
}
