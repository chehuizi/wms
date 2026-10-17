package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 仓库分页查询参数，支持 keyword、status。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseQuery extends BaseQuery {

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "code", "code",
                "name", "name",
                "status", "status",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
