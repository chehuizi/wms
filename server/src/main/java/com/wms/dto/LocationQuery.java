package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 库位分页查询参数，支持 keyword、status、warehouseId、locationType、zone。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LocationQuery extends BaseQuery {

    private Long warehouseId;
    private Integer locationType;
    private String zone;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "code", "code",
                "zone", "zone",
                "locationType", "location_type",
                "capacity", "capacity",
                "status", "status",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
