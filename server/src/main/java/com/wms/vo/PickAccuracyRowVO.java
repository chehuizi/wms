package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拣货准确率聚合结果。
 */
@Data
public class PickAccuracyRowVO {

    /** 一次拣对的数量 */
    private BigDecimal correctQty;

    /** 拣货总数量 */
    private BigDecimal totalPickQty;
}
