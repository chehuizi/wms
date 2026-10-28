package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 库区新建/更新请求体。
 */
@Data
public class ZoneSaveRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不能超过 32")
    private String code;

    @NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String name;
}
