package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 库区新增、编辑请求。
 */
@Data
public class ZoneSaveRequest {

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "库区编码不能为空")
    @Size(max = 32, message = "库区编码长度不能超过32")
    private String code;

    @NotBlank(message = "库区名称不能为空")
    @Size(max = 128, message = "库区名称长度不能超过128")
    private String name;
}
