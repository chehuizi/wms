package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 库区，隶属于仓库。
 */
@Data
public class Zone {

    private Long id;
    private Long warehouseId;
    private String code;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
