package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 库区分页查询参数，支持 keyword、warehouseId。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ZoneQuery extends BaseQuery {

    private Long warehouseId;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "warehouseId", "warehouse_id",
                "code", "code",
                "name", "name",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
