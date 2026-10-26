package com.wms.module.report.controller;

import com.wms.common.Result;
import com.wms.module.report.ReportService;
import com.wms.vo.DashboardVO;
import com.wms.vo.InboundOutboundVO;
import com.wms.vo.InventorySummaryVO;
import com.wms.vo.PickAccuracyVO;
import com.wms.vo.StockAgingVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 报表，见契约第 18 节。全部只读。
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(reportService.dashboard());
    }

    @GetMapping("/inventory-summary")
    public Result<InventorySummaryVO> inventorySummary(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String groupBy) {
        return Result.ok(reportService.inventorySummary(warehouseId, category, groupBy));
    }

    @GetMapping("/inbound-outbound")
    public Result<InboundOutboundVO> inboundOutbound(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String granularity) {
        return Result.ok(reportService.inboundOutbound(warehouseId, startDate, endDate, granularity));
    }

    @GetMapping("/stock-aging")
    public Result<StockAgingVO> stockAging(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String category) {
        return Result.ok(reportService.stockAging(warehouseId, category));
    }

    @GetMapping("/pick-accuracy")
    public Result<PickAccuracyVO> pickAccuracy() {
        return Result.ok(reportService.pickAccuracy());
    }
}
