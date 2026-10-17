package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建盘点单请求，见契约第 17 节。
 */
@Data
public class StocktakeCreateRequest {

    @NotNull(message = "不能为空")
    private Long warehouseId;

    /** 1=全盘 2=循环盘 3=抽盘 */
    @NotNull(message = "不能为空")
    private Integer stocktakeType;

    /** 全盘可空；循环盘/抽盘必填 */
    @Size(max = 32, message = "长度不能超过 32")
    private String scopeZone;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
