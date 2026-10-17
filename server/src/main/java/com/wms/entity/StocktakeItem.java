package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 盘点明细 wms_stocktake_item。status: 1=待盘 2=已盘 3=待审批 4=已调账。
 */
@Data
public class StocktakeItem {

    private Long id;
    private Long stocktakeId;
    private Long locationId;
    private Long skuId;
    private String batchNo;
    private Long inventoryId;
    private BigDecimal bookQty;
    private BigDecimal countedQty;
    private BigDecimal diffQty;
    private Integer status;
    private String countedBy;
    private OffsetDateTime countedAt;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
