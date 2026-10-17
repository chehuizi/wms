package com.wms.module.stock;

import com.wms.entity.Inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存写操作的唯一入口（契约第 10.4 / 10.5 / 10.7 条）。
 * <p>
 * 所有改 {@code wms_inventory} 的业务都必须走这里，保证三件事不会漏：
 * <ol>
 *   <li>先 {@code SELECT ... FOR UPDATE} 悲观加锁，防超卖</li>
 *   <li>同事务写一条 {@code wms_stock_movement}，带 before/after 快照</li>
 *   <li>校验库位是否允许负库存、锁定量是否超过在库量</li>
 * </ol>
 * 调用方必须处于事务中（本模块所有写服务的入口方法都是 {@code @Transactional}）。
 */
public interface StockService {

    /** 业务类型：入库 */
    String BIZ_INBOUND = "INBOUND";
    /** 业务类型：出库 */
    String BIZ_OUTBOUND = "OUTBOUND";
    /** 业务类型：盘点 */
    String BIZ_STOCKTAKE = "STOCKTAKE";
    /** 业务类型：手工调整 */
    String BIZ_MANUAL = "MANUAL";

    /**
     * 按唯一键（库位 + SKU + 批次 + 序列号）取库存行并加悲观锁；不存在则新建一条 0 库存行。
     */
    Inventory lockOrCreate(Long warehouseId, Long locationId, Long skuId,
                           String batchNo, String serialNo,
                           LocalDate producedDate, LocalDate expireDate);

    /**
     * 改在库量并记流水（movementType 由调用方指定）。
     *
     * @param locked 必须是通过 lockOrCreate / FOR UPDATE 查询拿到的已加锁行
     */
    Inventory changeOnHand(Inventory locked, BigDecimal qtyChange, int movementType,
                           BizRef biz, String operator, String remark);

    /**
     * 改锁定量并记流水。出库发货需要同时扣减在库量与锁定量，用
     * {@link #change(Inventory, BigDecimal, BigDecimal, int, BizRef, String, String)} 一次完成，只写一条流水。
     */
    Inventory changeLocked(Inventory locked, BigDecimal lockedChange, int movementType,
                           BizRef biz, String operator, String remark);

    /** 在库量与锁定量同时变化，只写一条流水（例如出库发货 2）。 */
    Inventory change(Inventory locked, BigDecimal qtyChange, BigDecimal lockedChange,
                     int movementType, BizRef biz, String operator, String remark);

    /**
     * 移库：同一事务内先写「5 移库出」再写「6 移库入」两条流水（契约第 10.7 条）。
     *
     * @param fromInventoryId 源库位库存行（已加锁）
     * @param toInventoryId   目标库位库存行（已加锁）
     */
    Inventory moveStock(Long fromInventoryId, Long toInventoryId, BigDecimal qty,
                        BizRef biz, String operator, String remark);
}
