package com.wms.vo;

import lombok.Data;

import java.util.List;

/**
 * 盘点单详情 = 单头 + 明细 + 调整记录，见契约第 17 节。
 */
@Data
public class StocktakeOrderDetailVO extends StocktakeOrderVO {

    private List<StocktakeItemVO> items;
    private List<StocktakeAdjustVO> adjusts;
}
