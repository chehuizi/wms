package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仓库新建/更新请求体，见契约第 4 节。
 */
@Data
public class WarehouseSaveRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不能超过 32")
    private String code;

    @NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String name;

    @Size(max = 255, message = "长度不能超过 255")
    private String address;

    @Size(max = 64, message = "长度不能超过 64")
    private String contactName;

    @Size(max = 32, message = "长度不能超过 32")
    private String contactPhone;

    /** 1=启用 0=停用，不传默认 1 */
    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
