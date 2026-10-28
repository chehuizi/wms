package com.wms.module.location;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 库位所属库区，用于在库位模块中标识库区。
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
