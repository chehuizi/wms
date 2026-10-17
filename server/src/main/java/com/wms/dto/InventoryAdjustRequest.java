package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 手工库存调整请求，见契约第 13 节 POST /inventories/adjust。
 */
@Data
public class InventoryAdjustRequest {

    /** 已存在则传；为 null 时按下面的键新建一条库存行 */
    private Long inventoryId;

    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private String batchNo;
    private String serialNo;

    /** 正数=盘盈，负数=盘亏，0 拒绝 */
    @NotNull(message = "不能为空")
    private BigDecimal qtyChange;

    @NotBlank(message = "不能为空")
    @Size(max = 255, message = "长度不能超过 255")
    private String reason;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String operator;
}
