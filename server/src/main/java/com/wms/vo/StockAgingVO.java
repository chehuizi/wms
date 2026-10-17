package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库龄分析，见契约第 18.4 节。
 */
@Data
public class StockAgingVO {

    private List<StockAgingRowVO> rows;
    private BigDecimal totalValue;
}
