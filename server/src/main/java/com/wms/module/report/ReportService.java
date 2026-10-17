package com.wms.module.report;

import com.wms.vo.DashboardVO;
import com.wms.vo.InboundOutboundVO;
import com.wms.vo.InventorySummaryVO;
import com.wms.vo.StockAgingVO;

import java.time.LocalDate;

/**
 * 报表查询，见契约第 18 节。全部只读，不新建任何表。
 */
public interface ReportService {

    DashboardVO dashboard();

    InventorySummaryVO inventorySummary(Long warehouseId, String category, String groupBy);

    InboundOutboundVO inboundOutbound(Long warehouseId, LocalDate startDate, LocalDate endDate,
                                       String granularity);

    StockAgingVO stockAging(Long warehouseId, String category);
}
