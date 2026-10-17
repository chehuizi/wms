package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 序列号分页查询参数，见契约第 14 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SerialQuery extends BaseQuery {

    private Long skuId;
    private Long warehouseId;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "serialNo", "serial_no",
                "createdAt", "created_at"
        );
    }
}
