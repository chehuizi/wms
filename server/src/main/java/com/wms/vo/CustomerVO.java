package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 客户响应对象，见契约第 7 节。
 */
@Data
public class CustomerVO {

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
}
