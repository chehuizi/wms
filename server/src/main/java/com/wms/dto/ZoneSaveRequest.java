package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库区新增、修改请求。
 */
@Data
public class ZoneSaveRequest {

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "库区编码不能为空")
    private String code;

    @NotBlank(message = "库区名称不能为空")
    private String name;

    private Integer status;
    private String remark;
}
