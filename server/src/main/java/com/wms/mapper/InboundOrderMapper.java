package com.wms.mapper;

import com.wms.dto.InboundOrderQuery;
import com.wms.entity.InboundItem;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundReceipt;
import com.wms.entity.InboundReceiptItem;
import com.wms.vo.InboundOrderVO;
import com.wms.vo.InboundReceiptItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 入库到货单 Mapper（单头 / 明细 / 收货记录 / 收货明细），SQL 定义在 resources/mapper/InboundOrderMapper.xml。
 */
@Mapper
public interface InboundOrderMapper {

    List<InboundOrderVO> selectPage(@Param("q") InboundOrderQuery query);

    long countPage(@Param("q") InboundOrderQuery query);

    InboundOrder selectById(@Param("id") Long id);

    /** 状态流转前加悲观锁，避免并发下同一单据被重复推进 */
    InboundOrder selectForUpdate(@Param("id") Long id);

    InboundOrderVO selectVOById(@Param("id") Long id);

    int insert(InboundOrder order);

    int update(InboundOrder order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markFinished(@Param("id") Long id, @Param("finishedAt") OffsetDateTime finishedAt);

    int softDeleteById(@Param("id") Long id);

    int deleteItems(@Param("inboundId") Long inboundId);

    // ---- 明细 ----

    int insertItem(InboundItem item);

    List<InboundItem> selectItems(@Param("inboundId") Long inboundId);

    /** 明细响应对象（含 SKU 编码/名称/单位） */
    List<com.wms.vo.InboundItemVO> selectItemVOs(@Param("inboundId") Long inboundId);

    InboundItem selectItemById(@Param("id") Long id);

    /** 收货累加已收数量前加锁 */
    InboundItem selectItemForUpdate(@Param("id") Long id);

    /** 累加已收数量 */
    int addReceivedQty(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);

    /** 是否所有明细都已收满（无明细返回 true） */
    int countUnfinishedItems(@Param("inboundId") Long inboundId);

    // ---- 收货记录 ----

    int insertReceipt(InboundReceipt receipt);

    List<InboundReceipt> selectReceipts(@Param("inboundId") Long inboundId);

    int insertReceiptItem(InboundReceiptItem item);

    List<InboundReceiptItemVO> selectReceiptItems(@Param("receiptId") Long receiptId);
}
