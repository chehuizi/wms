package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 出库发货请求，见契约第 16.5 节。
 */
@Data
public class ShipRequest {

    @Size(max = 64, message = "长度不能超过 64")
    private String carrier;

    @Size(max = 64, message = "长度不能超过 64")
    private String trackingNo;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String operator;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
