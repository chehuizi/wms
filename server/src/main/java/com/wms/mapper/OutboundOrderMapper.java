package com.wms.mapper;

import com.wms.dto.OutboundOrderQuery;
import com.wms.entity.Allocation;
import com.wms.entity.OutboundItem;
import com.wms.entity.OutboundOrder;
import com.wms.vo.AllocationVO;
import com.wms.vo.OutboundOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出库订单 Mapper（单头 / 明细 / 分配记录），SQL 定义在 resources/mapper/OutboundOrderMapper.xml。
 */
@Mapper
public interface OutboundOrderMapper {

    List<OutboundOrderVO> selectPage(@Param("q") OutboundOrderQuery query);

    long countPage(@Param("q") OutboundOrderQuery query);

    OutboundOrder selectById(@Param("id") Long id);

    /** 状态流转前加悲观锁 */
    OutboundOrder selectForUpdate(@Param("id") Long id);

    OutboundOrderVO selectVOById(@Param("id") Long id);

    int insert(OutboundOrder order);

    int update(OutboundOrder order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markShipped(@Param("id") Long id);

    int softDeleteById(@Param("id") Long id);

    int deleteItems(@Param("outboundId") Long outboundId);

    // ---- 明细 ----

    int insertItem(OutboundItem item);

    List<OutboundItem> selectItems(@Param("outboundId") Long outboundId);

    /** 明细响应对象（含 SKU 编码/名称/单位） */
    List<com.wms.vo.OutboundItemVO> selectItemVOs(@Param("outboundId") Long outboundId);

    OutboundItem selectItemById(@Param("id") Long id);

    /** 分配/拣货累加数量前加锁 */
    OutboundItem selectItemForUpdate(@Param("id") Long id);

    int addAllocatedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    int addPickedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    int addShippedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 是否所有明细都已拣满（无明细返回 true） */
    int countUnpickedItems(@Param("outboundId") Long outboundId);

    /** 是否所有明细都已分配满 */
    int countUnallocatedItems(@Param("outboundId") Long outboundId);

    // ---- 分配记录 ----

    int insertAllocation(Allocation allocation);

    List<Allocation> selectAllocations(@Param("outboundId") Long outboundId);

    Allocation selectAllocationById(@Param("id") Long id);

    /** 拣货/发货/释放前加锁 */
    Allocation selectAllocationForUpdate(@Param("id") Long id);

    List<Allocation> selectAllocationsByStatus(@Param("outboundId") Long outboundId,
                                               @Param("statuses") List<Integer> statuses);

    int addAllocationPickedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    int addAllocationShippedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    int markAllocationStatus(@Param("id") Long id, @Param("status") Integer status);

    List<AllocationVO> selectAllocationVOs(@Param("outboundId") Long outboundId);
}
