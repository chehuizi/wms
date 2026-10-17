package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 拣货任务分页查询参数，见契约第 16.4 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PickTaskQuery extends BaseQuery {

    private Long waveId;
    private Long outboundId;
    private Long skuId;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "taskNo", "task_no",
                "sequence", "sequence",
                "status", "status",
                "createdAt", "created_at"
        );
    }
}
