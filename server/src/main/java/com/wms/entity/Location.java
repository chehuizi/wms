package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库位 wms_location。
 */
@Data
public class Location {

    private Long id;
    private Long warehouseId;
    private String code;
    private Long zoneId;
    private String zone;
    /** 1=存储位 2=收货暂存位 3=发货暂存位 4=不良品位 5=退货位 */
    private Integer locationType;
    /** 容量，NULL 表示不限制 */
    private BigDecimal capacity;
    /** 1=允许负库存 0=不允许 */
    private Integer allowNegative;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
