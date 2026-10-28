package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 库区看板响应对象。
 */
@Data
public class ZoneVO {

    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private String code;
    private String name;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
