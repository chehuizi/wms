package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 盘点单分页查询参数，见契约第 17 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StocktakeOrderQuery extends BaseQuery {

    private Long warehouseId;
    /** 1=全盘 2=循环盘 3=抽盘 */
    private Integer stocktakeType;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "id",
                "orderNo", "order_no",
                "status", "status",
                "createdAt", "created_at",
                "updatedAt", "updated_at"
        );
    }
}
