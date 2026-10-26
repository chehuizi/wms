package com.wms.module.report.service;

import com.wms.common.BizException;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.ReportMapper;
import com.wms.module.report.ReportService;
import com.wms.vo.DashboardVO;
import com.wms.vo.InboundOutboundRowVO;
import com.wms.vo.InboundOutboundVO;
import com.wms.vo.InventorySummaryRowVO;
import com.wms.vo.InventorySummaryVO;
import com.wms.vo.PickAccuracyVO;
import com.wms.vo.StockAgingRowVO;
import com.wms.vo.StockAgingVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报表实现，见契约第 18 节。
 */
@Service
public class ReportServiceImpl implements ReportService {

    /** 库龄分桶顺序固定，缺桶补 0 */
    private static final List<String> AGING_BUCKETS = List.of("0-30天", "31-60天", "61-90天", "90天以上");

    /** 本版 SKU 无成本字段，库龄 value 固定为 0 */
    private static final BigDecimal ZERO_VALUE = BigDecimal.ZERO.setScale(2);

    private final ReportMapper reportMapper;
    private final InventoryMapper inventoryMapper;

    public ReportServiceImpl(ReportMapper reportMapper, InventoryMapper inventoryMapper) {
        this.reportMapper = reportMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setWarehouseCount(reportMapper.countWarehouse());
        vo.setLocationCount(reportMapper.countLocation());
        vo.setSkuCount(reportMapper.countSku());
        vo.setCustomerCount(reportMapper.countCustomer());

        var sum = inventoryMapper.sumInventory();
        vo.setTotalOnHandQty(sum == null || sum.getOnHandQty() == null ? BigDecimal.ZERO : sum.getOnHandQty());
        vo.setTotalAvailableQty(sum == null || sum.getAvailableQty() == null ? BigDecimal.ZERO : sum.getAvailableQty());

        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime start = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = start.plusDays(1);
        vo.setTodayInboundOrders(reportMapper.countInboundOrders(start, end));
        vo.setTodayOutboundOrders(reportMapper.countOutboundOrders(start, end));
        vo.setPendingPutawayTasks(reportMapper.countPendingPutawayTasks());
        vo.setPendingPickTasks(reportMapper.countPendingPickTasks());
        vo.setStocktakeInProgress(reportMapper.countStocktakeInProgress());
        vo.setExpiringSoonSkuCount(reportMapper.countExpiringSoonSku());
        return vo;
    }

    @Override
    public InventorySummaryVO inventorySummary(Long warehouseId, String category, String groupBy) {
        String group = groupBy == null || groupBy.isBlank() ? "warehouse" : groupBy;
        Set<String> allowed = Set.of("warehouse", "category", "sku");
        if (!allowed.contains(group)) {
            throw BizException.paramInvalid("groupBy 只能是 warehouse / category / sku: " + groupBy);
        }
        List<InventorySummaryRowVO> rows = reportMapper.selectInventorySummary(group, warehouseId, category);

        InventorySummaryVO vo = new InventorySummaryVO();
        BigDecimal onHand = BigDecimal.ZERO;
        BigDecimal locked = BigDecimal.ZERO;
        for (InventorySummaryRowVO row : rows) {
            onHand = onHand.add(nvl(row.getOnHandQty()));
            locked = locked.add(nvl(row.getLockedQty()));
        }
        vo.setRows(rows);
        vo.setTotalOnHandQty(onHand);
        vo.setTotalLockedQty(locked);
        vo.setTotalSkuCount(reportMapper.countSku());
        return vo;
    }

    @Override
    public InboundOutboundVO inboundOutbound(Long warehouseId, LocalDate startDate, LocalDate endDate,
                                              String granularity) {
        String grain = granularity == null || granularity.isBlank() ? "day" : granularity;
        Set<String> allowed = Set.of("day", "week", "month");
        if (!allowed.contains(grain)) {
            throw BizException.paramInvalid("granularity 只能是 day / week / month: " + granularity);
        }
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw BizException.paramInvalid("endDate 不能早于 startDate");
        }
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime from = start.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<InboundOutboundRowVO> rows = reportMapper.selectInboundOutbound(warehouseId, from, to, grain);

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (InboundOutboundRowVO row : rows) {
            totalIn = totalIn.add(nvl(row.getInboundQty()));
            totalOut = totalOut.add(nvl(row.getOutboundQty()));
        }
        InboundOutboundVO vo = new InboundOutboundVO();
        vo.setRows(rows);
        vo.setTotalInboundQty(totalIn);
        vo.setTotalOutboundQty(totalOut);
        return vo;
    }

    @Override
    public PickAccuracyVO pickAccuracy() {
        BigDecimal correctPickQty = nvl(reportMapper.sumCorrectPickQty());
        BigDecimal totalPickQty = nvl(reportMapper.sumTotalPickQty());

        PickAccuracyVO vo = new PickAccuracyVO();
        vo.setCorrectPickQty(correctPickQty);
        vo.setTotalPickQty(totalPickQty);
        vo.setPickAccuracy(totalPickQty.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2)
                : correctPickQty.multiply(BigDecimal.valueOf(100))
                        .divide(totalPickQty, 2, java.math.RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public StockAgingVO stockAging(Long warehouseId, String category) {
        List<InventorySummaryRowVO> raw = reportMapper.selectStockAging(warehouseId, category);

        Map<String, StockAgingRowVO> byBucket = new LinkedHashMap<>();
        for (String bucket : AGING_BUCKETS) {
            StockAgingRowVO row = new StockAgingRowVO();
            row.setBucket(bucket);
            row.setSkuCount(0L);
            row.setOnHandQty(BigDecimal.ZERO);
            row.setValue(ZERO_VALUE);
            byBucket.put(bucket, row);
        }
        for (InventorySummaryRowVO src : raw) {
            StockAgingRowVO target = byBucket.get(src.getGroupKey());
            if (target == null) {
                continue;
            }
            target.setSkuCount(src.getSkuCount());
            target.setOnHandQty(nvl(src.getOnHandQty()));
        }
        List<StockAgingRowVO> rows = new ArrayList<>(byBucket.values());
        StockAgingVO vo = new StockAgingVO();
        vo.setRows(rows);
        // 无成本字段，合计也固定为 0
        vo.setTotalValue(ZERO_VALUE);
        return vo;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
