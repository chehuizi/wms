package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 库存流水分页查询参数，见契约第 13 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MovementQuery extends BaseQuery {

    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private Integer movementType;
    private String bizType;
    private String bizNo;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "m.id",
                "movementNo", "m.movement_no",
                "createdAt", "m.created_at"
        );
    }
}
