package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 拣货任务 wms_pick_task。status: 1=待拣货 2=拣货中 3=已完成 4=已取消。
 */
@Data
public class PickTask {

    private Long id;
    private String taskNo;
    private Long waveId;
    private Long outboundId;
    private Long allocationId;
    private Long skuId;
    private Long fromLocationId;
    private Integer sequence;
    private BigDecimal requiredQty;
    private BigDecimal pickedQty;
    private Integer status;
    private String operator;
    private OffsetDateTime pickedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
