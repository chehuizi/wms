package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 盘点单 wms_stocktake_order。status: 1=草稿 2=进行中 3=待审批 4=已完成 5=已取消。
 */
@Data
public class StocktakeOrder {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    /** 1=全盘 2=循环盘 3=抽盘 */
    private Integer stocktakeType;
    private Integer status;
    private String scopeZone;
    private Integer totalLocations;
    private Integer countedLocations;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime approvedAt;
    private String approver;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
