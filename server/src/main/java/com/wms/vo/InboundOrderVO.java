package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 入库到货单响应对象，见契约第 15.1 节。
 */
@Data
public class InboundOrderVO {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private String supplierCode;
    private String supplierName;
    private Integer status;
    private OffsetDateTime expectedAt;
    private OffsetDateTime finishedAt;
    private String remark;
    /** 明细条数 */
    private Integer itemCount;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
