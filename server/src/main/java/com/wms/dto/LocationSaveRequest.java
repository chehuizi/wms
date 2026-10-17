package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库位新建/更新请求体，见契约第 5 节。
 */
@Data
public class LocationSaveRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不能超过 32")
    private String code;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不能超过 32")
    private String zone;

    /** 1=存储位 2=收货暂存位 3=发货暂存位 4=不良品位 5=退货位，不传默认 1 */
    private Integer locationType;

    /** 容量，>0，不传或 null 表示不限制 */
    @Positive(message = "必须大于 0")
    private BigDecimal capacity;

    /** 1=允许负库存 0=不允许，不传默认 0 */
    private Integer allowNegative;

    /** 1=启用 0=停用，不传默认 1 */
    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
