package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * SKU 响应对象，见契约第 6 节。
 */
@Data
public class SkuVO {

    private Long id;
    private String code;
    private String name;
    private String category;
    private String spec;
    private String unit;
    private String barcode;
    private Integer batchManaged;
    private Integer serialManaged;
    private Integer shelfLifeDays;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
