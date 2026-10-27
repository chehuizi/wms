package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 库区 wms_zone，库区是仓库下的分区，库位归属在库区上。
 */
@Data
public class Zone {

    private Long id;
    private Long warehouseId;
    private String code;
    private String name;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
