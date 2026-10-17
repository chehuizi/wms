package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 盘点审批调账请求，见契约第 17 节。
 */
@Data
public class StocktakeApproveRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String approver;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
