package com.wms.module.zone;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 库区下的库位，用于标识库区与库位的归属关系。
 */
@Data
public class Location {

    private Long id;
    private Long warehouseId;
    private String code;
    private String zone;
    private Integer locationType;
    private java.math.BigDecimal capacity;
    private Integer allowNegative;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer deleted;
}
