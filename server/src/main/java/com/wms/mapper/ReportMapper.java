package com.wms.mapper;

import com.wms.vo.InboundOutboundRowVO;
import com.wms.vo.InventorySummaryRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

/**
 * 报表 Mapper，全部为只读聚合查询，不新建任何表。
 * SQL 全部定义在 resources/mapper/ReportMapper.xml。
 */
@Mapper
public interface ReportMapper {

    long countWarehouse();

    long countLocation();

    long countSku();

    long countCustomer();

    /** 30 天内到期的 SKU 数 */
    long countExpiringSoonSku();

    long countInboundOrders(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    long countOutboundOrders(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    long countPendingPutawayTasks();

    long countPendingPickTasks();

    long countStocktakeInProgress();

    /** 拣货准确率：一次拣对数量和拣货总数量 */
    BigDecimal sumCorrectPickQty();

    BigDecimal sumTotalPickQty();

    /** 库存汇总分片：groupBy = warehouse / category / sku */
    List<InventorySummaryRowVO> selectInventorySummary(@Param("groupBy") String groupBy,
                                                       @Param("warehouseId") Long warehouseId,
                                                       @Param("category") String category);

    /** 出入库统计：granularity = day / week / month */
    List<InboundOutboundRowVO> selectInboundOutbound(@Param("warehouseId") Long warehouseId,
                                                     @Param("start") OffsetDateTime start,
                                                     @Param("end") OffsetDateTime end,
                                                     @Param("granularity") String granularity);

    /** 库龄分桶聚合：bucket 已在 SQL 中算好，value 固定 0 */
    List<InventorySummaryRowVO> selectStockAging(@Param("warehouseId") Long warehouseId,
                                                  @Param("category") String category);
}
