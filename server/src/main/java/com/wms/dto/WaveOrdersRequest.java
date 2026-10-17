package com.wms.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 波次加入订单请求，见契约第 16.3 节。
 */
@Data
public class WaveOrdersRequest {

    @NotEmpty(message = "至少选择 1 张订单")
    private List<Long> outboundIds;
}
