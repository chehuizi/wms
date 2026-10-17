package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 序列号响应对象，见契约第 14 节。
 */
@Data
public class SerialVO {

    private Long id;
    private String serialNo;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private Integer status;
    private OffsetDateTime inBoundAt;
    private OffsetDateTime outBoundAt;
    private OffsetDateTime createdAt;
}
