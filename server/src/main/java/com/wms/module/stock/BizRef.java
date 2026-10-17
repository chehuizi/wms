package com.wms.module.stock;

/**
 * 流水关联的业务单据引用。所有产生库存变化的写操作都必须带上它，
 * 方便按 biz_type / biz_no 反查账实。
 *
 * @param bizType 业务类型，如 INBOUND / OUTBOUND / STOCKTAKE / MANUAL
 * @param bizNo   业务单号，如 IN202610170001；手工调整没有单号时传 null
 */
public record BizRef(String bizType, String bizNo) {

    public static BizRef of(String bizType, String bizNo) {
        return new BizRef(bizType, bizNo);
    }
}
