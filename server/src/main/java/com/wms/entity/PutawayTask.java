package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 上架任务 wms_putaway_task。status: 1=待上架 2=已完成 3=已取消。
 *
 * producedDate / expireDate 是**批次的属性**，随货移动（契约第 15.2 节
 * 「批次和序列号随货移动」）。没有这两列，执行上架时库存行就拿不到效期 →
 * 货到了存储位效期没了 → FIFO 分配与临期预警一起失效。
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
    private LocalDate producedDate;
    private LocalDate expireDate;
    private BigDecimal qty;
    private Integer status;
    private String operator;
    private OffsetDateTime executedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
