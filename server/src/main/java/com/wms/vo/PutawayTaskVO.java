package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 上架任务响应对象，见契约第 15.2 节。
 */
@Data
public class PutawayTaskVO {

    private Long id;
    private String taskNo;
    private Long receiptItemId;
    private Long inboundId;
    private String inboundNo;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Long fromLocationId;
    private String fromLocationCode;
    private Long toLocationId;
    private String toLocationCode;
    private String batchNo;
    private String serialNo;
    private BigDecimal qty;
    private Integer status;
    private String operator;
    private OffsetDateTime executedAt;
    private OffsetDateTime createdAt;
}
