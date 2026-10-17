package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 盘点差异调整记录响应对象。
 */
@Data
public class StocktakeAdjustVO {

    private Long id;
    private Long stocktakeId;
    private Long stocktakeItemId;
    private Long inventoryId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Long locationId;
    private String locationCode;
    private BigDecimal diffQty;
    private String approvedBy;
    private OffsetDateTime approvedAt;
    private String remark;
}
