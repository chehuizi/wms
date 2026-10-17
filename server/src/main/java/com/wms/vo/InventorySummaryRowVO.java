package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存汇总行，见契约第 18.2 节。
 */
@Data
public class InventorySummaryRowVO {

    private String groupKey;
    private String groupName;
    private Long skuCount;
    private BigDecimal onHandQty;
    private BigDecimal lockedQty;
    private BigDecimal availableQty;
}
