package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 仓库 wms_warehouse。
 */
@Data
public class Warehouse {

    private Long id;
    private String code;
    private String name;
    private String address;
    private String contactName;
    private String contactPhone;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
