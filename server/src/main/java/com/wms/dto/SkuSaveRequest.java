package com.wms.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 新建/更新请求体，见契约第 6 节。
 */
@Data
public class SkuSaveRequest {

    @jakarta.validation.constraints.NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String code;

    @jakarta.validation.constraints.NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String name;

    @Size(max = 64, message = "长度不能超过 64")
    private String category;

    @Size(max = 64, message = "长度不能超过 64")
    private String spec;

    @Size(max = 16, message = "长度不能超过 16")
    private String unit;

    @Size(max = 64, message = "长度不能超过 64")
    private String barcode;

    /** 1=批次管理 0=否，不传默认 0 */
    private Integer batchManaged;

    /** 1=序列号管理 0=否，不传默认 0 */
    private Integer serialManaged;

    /** 保质期天数，>=0，0 视同无保质期后端存 NULL */
    @PositiveOrZero(message = "不能小于 0")
    private Integer shelfLifeDays;

    @PositiveOrZero(message = "不能小于 0")
    private BigDecimal weight;

    @PositiveOrZero(message = "不能小于 0")
    private BigDecimal volume;

    @PositiveOrZero(message = "不能小于 0")
    private BigDecimal length;

    @PositiveOrZero(message = "不能小于 0")
    private BigDecimal width;

    @PositiveOrZero(message = "不能小于 0")
    private BigDecimal height;

    /** 1=启用 0=停用，不传默认 1 */
    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
