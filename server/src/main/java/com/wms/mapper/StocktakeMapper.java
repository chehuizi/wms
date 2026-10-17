package com.wms.mapper;

import com.wms.dto.StocktakeOrderQuery;
import com.wms.entity.StocktakeAdjust;
import com.wms.entity.StocktakeItem;
import com.wms.entity.StocktakeOrder;
import com.wms.vo.StocktakeAdjustVO;
import com.wms.vo.StocktakeItemVO;
import com.wms.vo.StocktakeOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点 Mapper（单头 / 明细 / 差异调整），SQL 定义在 resources/mapper/StocktakeMapper.xml。
 */
@Mapper
public interface StocktakeMapper {

    List<StocktakeOrderVO> selectPage(@Param("q") StocktakeOrderQuery query);

    long countPage(@Param("q") StocktakeOrderQuery query);

    StocktakeOrder selectById(@Param("id") Long id);

    /** 状态流转前加悲观锁 */
    StocktakeOrder selectForUpdate(@Param("id") Long id);

    StocktakeOrderVO selectVOById(@Param("id") Long id);

    int insert(StocktakeOrder order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markStarted(@Param("id") Long id);

    int markFinished(@Param("id") Long id);

    int markApproved(@Param("id") Long id, @Param("approver") String approver);

    int markCancelled(@Param("id") Long id);

    int softDeleteById(@Param("id") Long id);

    int deleteItems(@Param("stocktakeId") Long stocktakeId);

    int setTotalLocations(@Param("id") Long id, @Param("totalLocations") Integer totalLocations);

    // ---- 明细 ----

    /** 按范围内所有 on_hand_qty > 0 的库存行批量生成盘点明细，bookQty 为生成时刻快照 */
    int insertItemsFromInventory(@Param("stocktakeId") Long stocktakeId,
                                 @Param("warehouseId") Long warehouseId,
                                 @Param("scopeZone") String scopeZone);

    List<StocktakeItemVO> selectItems(@Param("stocktakeId") Long stocktakeId);

    /** 明细实体列表，审批调账时逐条加锁处理 */
    List<StocktakeItem> selectItemEntities(@Param("stocktakeId") Long stocktakeId);

    /** 按已盘明细回写已盘库位数 */
    int refreshCountedLocations(@Param("stocktakeId") Long stocktakeId);

    StocktakeItem selectItemById(@Param("id") Long id);

    /** 调账前加悲观锁并取原始明细 */
    StocktakeItem selectItemForUpdate(@Param("id") Long id);

    /** 录入实盘，diffQty = countedQty - bookQty */
    int updateCounted(@Param("id") Long id,
                      @Param("countedQty") BigDecimal countedQty,
                      @Param("diffQty") BigDecimal diffQty,
                      @Param("countedBy") String countedBy,
                      @Param("remark") String remark);

    int markItemStatus(@Param("id") Long id, @Param("status") Integer status);

    int markAllItemsStatus(@Param("stocktakeId") Long stocktakeId, @Param("status") Integer status);

    int countItems(@Param("stocktakeId") Long stocktakeId);

    int countUncountedItems(@Param("stocktakeId") Long stocktakeId);

    int countDiffItems(@Param("stocktakeId") Long stocktakeId);

    int countCountedLocations(@Param("stocktakeId") Long stocktakeId);

    BigDecimal sumDiffQty(@Param("stocktakeId") Long stocktakeId);

    // ---- 差异调整 ----

    int insertAdjust(StocktakeAdjust adjust);

    List<StocktakeAdjustVO> selectAdjusts(@Param("stocktakeId") Long stocktakeId);
}
