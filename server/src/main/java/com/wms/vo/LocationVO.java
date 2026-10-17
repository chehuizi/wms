package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库位响应对象，比请求体多 warehouseName，见契约第 5 节。
 */
@Data
public class LocationVO {

    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private String code;
    private String zone;
    private Integer locationType;
    private BigDecimal capacity;
    private Integer allowNegative;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
