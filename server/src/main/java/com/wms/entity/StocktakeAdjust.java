package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 盘点差异调整记录 wms_stocktake_adjust。
 */
@Data
public class StocktakeAdjust {

    private Long id;
    private Long stocktakeId;
    private Long stocktakeItemId;
    private Long inventoryId;
    private Long skuId;
    private Long locationId;
    private BigDecimal diffQty;
    private String approvedBy;
    private OffsetDateTime approvedAt;
    private String remark;
    private OffsetDateTime createdAt;
}
