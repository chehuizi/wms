package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 出库订单新建/修改请求，见契约第 16.1 节。
 */
@Data
public class OutboundOrderSaveRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    private Long customerId;

    /** 优先级，数值越大越优先，默认 0 */
    private Integer priority;

    private OffsetDateTime expectedShipAt;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;

    @NotEmpty(message = "至少 1 条明细")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "不能为空")
        private Long skuId;

        @NotNull(message = "不能为空")
        @Positive(message = "必须大于 0")
        private BigDecimal orderedQty;
    }
}
