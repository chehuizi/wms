package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收货明细响应对象。
 */
@Data
public class InboundReceiptItemVO {

    private Long id;
    private Long receiptId;
    private Long inboundItemId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String serialNo;
    private LocalDate producedDate;
    private LocalDate expireDate;
    private BigDecimal qty;
}
