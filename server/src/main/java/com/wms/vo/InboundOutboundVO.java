package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出入库统计，见契约第 18.3 节。
 */
@Data
public class InboundOutboundVO {

    private List<InboundOutboundRowVO> rows;
    private BigDecimal totalInboundQty;
    private BigDecimal totalOutboundQty;
}
