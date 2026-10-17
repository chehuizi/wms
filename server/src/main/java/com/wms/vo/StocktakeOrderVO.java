package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 盘点单响应对象，见契约第 17 节。
 */
@Data
public class StocktakeOrderVO {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private Integer stocktakeType;
    private Integer status;
    private String scopeZone;
    private Integer totalLocations;
    private Integer countedLocations;
    private Integer totalItems;
    private Integer countedItems;
    private Integer diffItems;
    private BigDecimal diffQty;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime approvedAt;
    private String approver;
    private String remark;
    private OffsetDateTime createdAt;
}
