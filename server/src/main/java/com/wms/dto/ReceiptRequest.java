package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库收货请求，见契约第 15.2 节。
 */
@Data
public class ReceiptRequest {

    /** 收货暂存位，必须是该仓库下 locationType=2 的启用库位 */
    @NotNull(message = "不能为空")
    private Long locationId;

    @Size(max = 64, message = "长度不能超过 64")
    private String operator;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;

    @NotEmpty(message = "至少 1 条明细")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "不能为空")
        private Long inboundItemId;

        private String batchNo;

        /** SKU 开序列号管理时必填 */
        private String serialNo;

        private LocalDate producedDate;
        private LocalDate expireDate;

        @NotNull(message = "不能为空")
        @Positive(message = "必须大于 0")
        private BigDecimal qty;
    }
}
