package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * SKU 分页查询参数，支持 keyword、status、category、batchManaged、serialManaged。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SkuQuery extends BaseQuery {

    private String category;
    private Integer batchManaged;
    private Integer serialManaged;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "code", "code",
                "name", "name",
                "category", "category",
                "status", "status",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
