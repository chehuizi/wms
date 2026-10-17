package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 序列号台账 wms_serial。status: 1=在库 2=已锁定 3=已出库 4=已退货。
 */
@Data
public class Serial {

    private Long id;
    private String serialNo;
    private Long skuId;
    private Long warehouseId;
    private Long locationId;
    private Integer status;
    private OffsetDateTime inBoundAt;
    private OffsetDateTime outBoundAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
