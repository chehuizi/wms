package com.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库存流水 wms_stock_movement，只增不改不删（契约第 10.6 条）。
 */
@Data
public class StockMovement {

    private Long id;
    private String movementNo;
    /** 1=入库收货 2=出库发货 3=盘盈 4=盘亏 5=移库出 6=移库入 7=锁定 8=解锁 9=手工调整 */
    private Integer movementType;
    private String bizType;
    private String bizNo;
    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private String batchNo;
    private String serialNo;
    private BigDecimal qtyChange;
    private BigDecimal lockedChange;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private String operator;
    private String remark;
    private OffsetDateTime createdAt;
}
