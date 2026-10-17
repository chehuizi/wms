package com.wms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户新建/更新请求体，见契约第 7 节。
 */
@Data
public class CustomerSaveRequest {

    @jakarta.validation.constraints.NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不能超过 32")
    private String code;

    @jakarta.validation.constraints.NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String name;

    @Size(max = 64, message = "长度不能超过 64")
    private String contactName;

    @Size(max = 32, message = "长度不能超过 32")
    private String contactPhone;

    /**
     * 邮箱，选填。@Email 对 null 放行；空串在 Service 里归一化为 null 后再入库。
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "长度不能超过 128")
    private String contactEmail;

    @Size(max = 255, message = "长度不能超过 255")
    private String address;

    /** 1=启用 0=停用，不传默认 1 */
    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
