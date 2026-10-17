package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拣货请求，见契约第 16.4 节。
 */
@Data
public class PickRequest {

    /** 必填，> 0 且 <= requiredQty，允许部分拣 */
    @NotNull(message = "不能为空")
    @Positive(message = "必须大于 0")
    private BigDecimal pickedQty;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String operator;
}
