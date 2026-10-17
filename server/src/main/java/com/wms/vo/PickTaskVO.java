package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 拣货任务响应对象，见契约第 16.4 节。
 */
@Data
public class PickTaskVO {

    private Long id;
    private String taskNo;
    private Long waveId;
    private String waveNo;
    private Long outboundId;
    private String outboundNo;
    private Long allocationId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Long fromLocationId;
    private String fromLocationCode;
    private Integer sequence;
    private BigDecimal requiredQty;
    private BigDecimal pickedQty;
    private Integer status;
    private String operator;
    private OffsetDateTime pickedAt;
    private OffsetDateTime createdAt;
}
