package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 上架任务分页查询参数，见契约第 15.2 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PutawayTaskQuery extends BaseQuery {

    private Long inboundId;
    private Long skuId;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "taskNo", "task_no",
                "status", "status",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
