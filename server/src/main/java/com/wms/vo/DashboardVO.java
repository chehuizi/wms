package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板汇总，见契约第 18.1 节。
 */
@Data
public class DashboardVO {

    private Long warehouseCount;
    private Long locationCount;
    private Long skuCount;
    private Long customerCount;
    private BigDecimal totalOnHandQty;
    private BigDecimal totalAvailableQty;
    private Long todayInboundOrders;
    private Long todayOutboundOrders;
    private Long pendingPutawayTasks;
    private Long pendingPickTasks;
    private Long stocktakeInProgress;
    /** 30 天内到期 */
    private Long expiringSoonSkuCount;
    /** 拣货准确率：一次拣对数量 / 拣货总数量 */
    private BigDecimal pickAccuracy;
}
