package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 录入实盘请求，见契约第 17 节。没货要显式录 0，不允许不填。
 */
@Data
public class StocktakeCountRequest {

    @NotEmpty(message = "至少 1 条明细")
    @Valid
    private List<Item> items;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String operator;

    @Data
    public static class Item {

        @NotNull(message = "不能为空")
        private Long itemId;

        @NotNull(message = "不能为空")
        @PositiveOrZero(message = "不能小于 0")
        private BigDecimal countedQty;

        @Size(max = 255, message = "长度不能超过 255")
        private String remark;
    }
}
