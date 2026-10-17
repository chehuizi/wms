package com.wms.module.stock.service;

import com.wms.common.BizException;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.entity.StockMovement;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.StockMovementMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存写操作实现，见契约第 10 节 8 条核心不变式。
 * <p>
 * 这是唯一允许改 wms_inventory 的地方：先悲观加锁，再校验，最后同事务写流水。
 */
@Service
public class StockServiceImpl implements StockService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper movementMapper;
    private final LocationMapper locationMapper;
    private final DocNoGenerator docNoGenerator;

    public StockServiceImpl(InventoryMapper inventoryMapper,
                            StockMovementMapper movementMapper,
                            LocationMapper locationMapper,
                            DocNoGenerator docNoGenerator) {
        this.inventoryMapper = inventoryMapper;
        this.movementMapper = movementMapper;
        this.locationMapper = locationMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Override
    public Inventory lockOrCreate(Long warehouseId, Long locationId, Long skuId,
                                  String batchNo, String serialNo,
                                  LocalDate producedDate, LocalDate expireDate) {
        Inventory existing = inventoryMapper.selectForUpdateByKey(locationId, skuId, batchNo, serialNo);
        if (existing != null) {
            return existing;
        }
        Inventory fresh = new Inventory();
        fresh.setWarehouseId(warehouseId);
        fresh.setLocationId(locationId);
        fresh.setSkuId(skuId);
        fresh.setBatchNo(batchNo);
        fresh.setSerialNo(serialNo);
        fresh.setProducedDate(producedDate);
        fresh.setExpireDate(expireDate);
        fresh.setOnHandQty(ZERO);
        fresh.setLockedQty(ZERO);
        fresh.setStatus(1);
        fresh.setDeleted(0);
        try {
            inventoryMapper.insert(fresh);
        } catch (DuplicateKeyException e) {
            // 并发下另一事务刚建好同一唯一键的行，重新加锁取它
            Inventory concurrent = inventoryMapper.selectForUpdateByKey(locationId, skuId, batchNo, serialNo);
            if (concurrent != null) {
                return concurrent;
            }
            throw e;
        }
        return fresh;
    }

    @Override
    public Inventory changeOnHand(Inventory locked, BigDecimal qtyChange, int movementType,
                                  BizRef biz, String operator, String remark) {
        return change(locked, qtyChange, ZERO, movementType, biz, operator, remark);
    }

    @Override
    public Inventory changeLocked(Inventory locked, BigDecimal lockedChange, int movementType,
                                  BizRef biz, String operator, String remark) {
        return change(locked, ZERO, lockedChange, movementType, biz, operator, remark);
    }

    @Override
    public Inventory change(Inventory locked, BigDecimal qtyChange, BigDecimal lockedChange,
                            int movementType, BizRef biz, String operator, String remark) {
        if (locked == null || locked.getId() == null) {
            throw BizException.paramInvalid("库存行未加锁，不能变更库存");
        }
        BigDecimal qtyDelta = nvl(qtyChange);
        BigDecimal lockedDelta = nvl(lockedChange);
        if (qtyDelta.signum() == 0 && lockedDelta.signum() == 0) {
            return locked;
        }
        BigDecimal beforeOnHand = nvl(locked.getOnHandQty());
        BigDecimal beforeLocked = nvl(locked.getLockedQty());
        BigDecimal afterOnHand = beforeOnHand.add(qtyDelta);
        BigDecimal afterLocked = beforeLocked.add(lockedDelta);

        // 不变式 3 / 8：库位不允许负库存。allowNegative=1 只放宽该校验，不放宽过期校验。
        // 注意：V3 表上有无条件 CHECK (on_hand_qty >= 0)，即使 allowNegative=1 也存不下负库存，
        // 因此这里一律按 40904 拦截，不让请求穿透到 DB 报 500。
        if (afterOnHand.signum() < 0) {
            throw BizException.negativeNotAllowed(
                    "库位 " + locationCode(locked.getLocationId()) + " 不允许负库存："
                            + "现有 " + beforeOnHand + "，本次扣减 " + qtyDelta.negate());
        }
        if (afterLocked.signum() < 0) {
            throw BizException.paramInvalid("锁定量不能为负：现有 " + beforeLocked + "，本次变化 " + lockedDelta);
        }
        // 不变式 2：锁定量 ≤ 在库量（DB 也有 ck_inventory_not_over_locked，这里提前拦成业务错误）
        if (afterLocked.compareTo(afterOnHand) > 0) {
            throw BizException.paramInvalid(
                    "锁定量不得超过在库量：在库 " + afterOnHand + "，锁定 " + afterLocked);
        }

        inventoryMapper.updateQty(locked.getId(), afterOnHand, afterLocked);

        // 不变式 4 / 6：同事务写流水，带变动前后快照
        StockMovement movement = new StockMovement();
        movement.setMovementNo(docNoGenerator.next(DocNoGenerator.MOVEMENT));
        movement.setMovementType(movementType);
        movement.setBizType(biz == null ? null : biz.bizType());
        movement.setBizNo(biz == null ? null : biz.bizNo());
        movement.setWarehouseId(locked.getWarehouseId());
        movement.setLocationId(locked.getLocationId());
        movement.setSkuId(locked.getSkuId());
        movement.setBatchNo(locked.getBatchNo());
        movement.setSerialNo(locked.getSerialNo());
        movement.setQtyChange(qtyDelta);
        movement.setLockedChange(lockedDelta);
        movement.setBeforeQty(beforeOnHand);
        movement.setAfterQty(afterOnHand);
        movement.setOperator(operator);
        movement.setRemark(remark);
        movementMapper.insert(movement);

        locked.setOnHandQty(afterOnHand);
        locked.setLockedQty(afterLocked);
        return locked;
    }

    @Override
    public Inventory moveStock(Long fromInventoryId, Long toInventoryId, BigDecimal qty,
                               BizRef biz, String operator, String remark) {
        Inventory from = requireLockedRow(fromInventoryId, "移库源库位");
        Inventory to = requireLockedRow(toInventoryId, "移库目标库位");
        if (from.getId().equals(to.getId())) {
            throw BizException.paramInvalid("移库源库位与目标库位不能相同");
        }
        if (nvl(qty).signum() <= 0) {
            throw BizException.paramInvalid("移库数量必须大于 0");
        }
        // 不变式 7：先写 5 移库出，再写 6 移库入
        changeOnHand(from, qty.negate(), MovementType.MOVE_OUT, biz, operator, remark);
        return changeOnHand(to, qty, MovementType.MOVE_IN, biz, operator, remark);
    }

    private Inventory requireLockedRow(Long inventoryId, String scene) {
        Inventory row = inventoryMapper.selectByIdForUpdate(inventoryId);
        if (row == null) {
            throw BizException.notFound(scene + "库存不存在: " + inventoryId);
        }
        return row;
    }

    private String locationCode(Long locationId) {
        Location location = locationMapper.selectById(locationId);
        return location == null ? String.valueOf(locationId) : location.getCode();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
