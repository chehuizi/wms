package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 商品 SKU wms_sku。
 */
@Data
public class Sku {

    private Long id;
    private String code;
    private String name;
    private String category;
    private String spec;
    private String unit;
    private String barcode;
    private Integer batchManaged;
    private Integer serialManaged;
    /** 保质期天数，NULL 表示无保质期 */
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
    private Integer deleted;
}
