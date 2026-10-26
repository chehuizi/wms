package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拣货准确率报表。
 * 一次拣对数量 ÷ 拣货总数量。
 */
@Data
public class PickAccuracyVO {

    /** 一次拣对数量。 */
    private BigDecimal correctQty;

    /** 拣货总数量。 */
    private BigDecimal totalPickQty;

    /** 拣货准确率，按百分比返回。 */
    private BigDecimal accuracyRate;
}
