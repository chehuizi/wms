package com.wms.mapper;

import com.wms.dto.InventoryQuery;
import com.wms.entity.Inventory;
import com.wms.vo.InventoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存台账 Mapper，SQL 全部定义在 resources/mapper/InventoryMapper.xml。
 */
@Mapper
public interface InventoryMapper {

    List<InventoryVO> selectPage(@Param("q") InventoryQuery query);

    long countPage(@Param("q") InventoryQuery query);

    /** 单条详情（含关联名称） */
    InventoryVO selectVOById(@Param("id") Long id);

    /** 原始库存行 */
    Inventory selectById(@Param("id") Long id);

    /** 原始库存行并悲观加锁，移库按 id 取源/目标行时使用 */
    Inventory selectByIdForUpdate(@Param("id") Long id);

    /**
     * 按唯一键取库存行并悲观加锁（FOR UPDATE）。
     * batchNo / serialNo 用 IS NOT DISTINCT FROM 匹配，保证 NULL 与 NULL 视为同一个值。
     */
    Inventory selectForUpdateByKey(@Param("locationId") Long locationId,
                                   @Param("skuId") Long skuId,
                                   @Param("batchNo") String batchNo,
                                   @Param("serialNo") String serialNo);

    int insert(Inventory inventory);

    /** 写入新的在库量与锁定量 */
    int updateQty(@Param("id") Long id,
                  @Param("onHandQty") BigDecimal onHandQty,
                  @Param("lockedQty") BigDecimal lockedQty);

    /**
     * FIFO 取可用库存：先临期（expire_date 早的，NULL 最后），再生产日期早的，最后 id 小的。
     * 只取 status = 1 且未删除的行，并 FOR UPDATE 悲观加锁。
     */
    List<Inventory> selectAvailableForSkuFifo(@Param("warehouseId") Long warehouseId,
                                              @Param("skuId") Long skuId);

    /** 全仓在库量与可用量合计 */
    Sum sumInventory();

    /** 看板/汇总用的合计投影 */
    class Sum {

        private BigDecimal onHandQty;
        private BigDecimal availableQty;

        public BigDecimal getOnHandQty() {
            return onHandQty;
        }

        public BigDecimal getAvailableQty() {
            return availableQty;
        }
    }
}
