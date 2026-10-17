package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 入库到货单新建/修改请求，见契约第 15.1 节。
 */
@Data
public class InboundOrderSaveRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    @Size(max = 32, message = "长度不能超过 32")
    private String supplierCode;

    @Size(max = 128, message = "长度不能超过 128")
    private String supplierName;

    private OffsetDateTime expectedAt;

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
        private java.math.BigDecimal expectedQty;
    }
}
