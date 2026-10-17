package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 仓库响应对象，见契约第 4 节。
 */
@Data
public class WarehouseVO {

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
}
