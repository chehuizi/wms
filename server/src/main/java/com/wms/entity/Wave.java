package com.wms.entity;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 拣货波次 wms_wave。status: 1=创建中 2=已释放 3=拣货中 4=已完成 5=已关闭。
 */
@Data
public class Wave {

    private Long id;
    private String waveNo;
    private Long warehouseId;
    private Integer status;
    private Integer orderCount;
    private OffsetDateTime releasedAt;
    private OffsetDateTime finishedAt;
    private String operator;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
