package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库龄分析行，见契约第 18.4 节。
 */
@Data
public class StockAgingRowVO {

    private String bucket;
    private Long skuCount;
    private BigDecimal onHandQty;
    /** 本版 SKU 无成本字段，固定返回 0 */
    private BigDecimal value;
}
