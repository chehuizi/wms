package com.wms.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 入库收货记录响应对象，见契约第 15.2 节。
 */
@Data
public class InboundReceiptVO {

    private Long id;
    private String receiptNo;
    private Long inboundId;
    private Long locationId;
    private String locationCode;
    private OffsetDateTime receivedAt;
    private String operator;
    private String remark;
    private List<InboundReceiptItemVO> items;
}
