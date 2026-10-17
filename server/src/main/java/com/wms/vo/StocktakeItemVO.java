package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 盘点明细响应对象。
 */
@Data
public class StocktakeItemVO {

    private Long id;
    private Long stocktakeId;
    private Long locationId;
    private String locationCode;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String unit;
    private String batchNo;
    private Long inventoryId;
    private BigDecimal bookQty;
    private BigDecimal countedQty;
    private BigDecimal diffQty;
    private Integer status;
    private String countedBy;
    private OffsetDateTime countedAt;
    private String remark;
}
