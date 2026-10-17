package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 客户 wms_customer。
 */
@Data
public class Customer {

    private Long id;
    private String code;
    private String name;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
