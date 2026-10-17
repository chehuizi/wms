package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 出库订单分页查询参数，见契约第 16.1 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundOrderQuery extends BaseQuery {

    private Long warehouseId;
    private Long customerId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "orderNo", "order_no",
                "status", "status",
                "priority", "priority",
                "expectedShipAt", "expected_ship_at",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
