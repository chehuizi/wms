package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 执行上架请求，见契约第 15.2 节。
 */
@Data
public class PutawayExecuteRequest {

    /** 必须是该仓库下 locationType=1 存储位的启用库位 */
    @NotNull(message = "不能为空")
    private Long toLocationId;

    @Size(max = 64, message = "长度不能超过 64")
    private String operator;
}
