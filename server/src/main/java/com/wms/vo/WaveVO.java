package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 拣货波次响应对象，见契约第 16.3 节。
 */
@Data
public class WaveVO {

    private Long id;
    private String waveNo;
    private Long warehouseId;
    private String warehouseName;
    private Integer status;
    private Integer orderCount;
    private OffsetDateTime releasedAt;
    private OffsetDateTime finishedAt;
    private String operator;
    private OffsetDateTime createdAt;
}
