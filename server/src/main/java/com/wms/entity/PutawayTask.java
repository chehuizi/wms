package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 上架任务 wms_putaway_task。status: 1=待上架 2=已完成 3=已取消。
 */
@Data
public class PutawayTask {

    private Long id;
    private String taskNo;
    private Long receiptItemId;
    private Long inboundId;
    private Long skuId;
    private Long fromLocationId;
    private Long toLocationId;
    private String batchNo;
    private String serialNo;
    private BigDecimal qty;
    private Integer status;
    private String operator;
    private OffsetDateTime executedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
