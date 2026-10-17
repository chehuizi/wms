package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 库存台账分页查询参数，见契约第 13 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryQuery extends BaseQuery {

    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    /** 商品分类 */
    private String category;
    private String batchNo;
    /** 只看在库量 > 0 的，默认 false */
    private Boolean onlyAvailable;
    /** 是否包含已锁完的（on_hand > 0 且 available = 0），默认 true */
    private Boolean includeLockedZero;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "i.id",
                "onHandQty", "i.on_hand_qty",
                "lockedQty", "i.locked_qty",
                "updatedAt", "i.updated_at",
                "createdAt", "i.created_at"
        );
    }

    public boolean isOnlyAvailableResolved() {
        return Boolean.TRUE.equals(onlyAvailable);
    }

    public boolean isIncludeLockedZeroResolved() {
        return includeLockedZero == null || includeLockedZero;
    }
}
