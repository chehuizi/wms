package com.wms.module.stock;

/**
 * 库存流水类型，取值见契约第 12.3 节。
 */
public final class MovementType {

    /** 入库收货 */
    public static final int INBOUND_RECEIPT = 1;
    /** 出库发货 */
    public static final int OUTBOUND_SHIP = 2;
    /** 盘盈 */
    public static final int STOCK_GAIN = 3;
    /** 盘亏 */
    public static final int STOCK_LOSS = 4;
    /** 移库出 */
    public static final int MOVE_OUT = 5;
    /** 移库入 */
    public static final int MOVE_IN = 6;
    /** 锁定 */
    public static final int LOCK = 7;
    /** 解锁 */
    public static final int UNLOCK = 8;
    /** 手工调整 */
    public static final int MANUAL_ADJUST = 9;

    private MovementType() {
    }
}
