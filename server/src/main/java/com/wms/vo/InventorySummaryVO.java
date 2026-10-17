package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存汇总，见契约第 18.2 节。
 */
@Data
public class InventorySummaryVO {

    private Long totalSkuCount;
    private BigDecimal totalOnHandQty;
    private BigDecimal totalLockedQty;
    private List<InventorySummaryRowVO> rows;
}
