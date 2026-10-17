package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 入库到货单分页查询参数，见契约第 15.1 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InboundOrderQuery extends BaseQuery {

    private Long warehouseId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "orderNo", "order_no",
                "status", "status",
                "expectedAt", "expected_at",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
