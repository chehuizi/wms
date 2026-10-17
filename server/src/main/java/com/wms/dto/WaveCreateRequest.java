package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建波次请求，见契约第 16.3 节。
 */
@Data
public class WaveCreateRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    @Size(max = 64, message = "长度不能超过 64")
    private String operator;
}
