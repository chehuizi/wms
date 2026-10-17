package com.wms.module.common;

import com.wms.mapper.DocNoMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 单据号生成器，规则见契约第 11 节：前缀 + yyyyMMdd + 日内序号。
 * <p>
 * 不新建序列表：先取事务级 advisory lock 把同前缀的取号串行化，
 * 再取当天已用的最大序号 + 1。
 * <p>
 * 注意：调用方必须处于事务中（{@code pg_advisory_xact_lock} 随事务释放）。
 * 流水号 MV 按契约示例为 5 位（MV2026101700001），其余单据为 4 位。
 */
@Component
public class DocNoGenerator {

    public static final String INBOUND_ORDER = "IN";
    public static final String RECEIPT = "RC";
    public static final String PUTAWAY_TASK = "PT";
    public static final String OUTBOUND_ORDER = "OUT";
    public static final String PICK_TASK = "PK";
    public static final String SHIPMENT = "SH";
    public static final String WAVE = "WV";
    public static final String STOCKTAKE_ORDER = "ST";
    public static final String MOVEMENT = "MV";

    /** 前缀 -> (表名, 单号列, 序号位数)，白名单，杜绝外部输入进入 ${} 拼接 */
    private static final Map<String, Target> TARGETS = Map.of(
            INBOUND_ORDER, new Target("wms_inbound_order", "order_no", 4),
            RECEIPT, new Target("wms_inbound_receipt", "receipt_no", 4),
            PUTAWAY_TASK, new Target("wms_putaway_task", "task_no", 4),
            OUTBOUND_ORDER, new Target("wms_outbound_order", "order_no", 4),
            PICK_TASK, new Target("wms_pick_task", "task_no", 4),
            SHIPMENT, new Target("wms_shipment", "shipment_no", 4),
            WAVE, new Target("wms_wave", "wave_no", 4),
            STOCKTAKE_ORDER, new Target("wms_stocktake_order", "order_no", 4),
            MOVEMENT, new Target("wms_stock_movement", "movement_no", 5)
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final DocNoMapper docNoMapper;

    public DocNoGenerator(DocNoMapper docNoMapper) {
        this.docNoMapper = docNoMapper;
    }

    /**
     * 生成下一个单据号。必须在事务内调用。
     */
    public String next(String prefix) {
        Target target = TARGETS.get(prefix);
        if (target == null) {
            throw new IllegalArgumentException("未登记的单据前缀: " + prefix);
        }
        docNoMapper.lockDocNo("wms-doc-no:" + prefix);
        // base 是真正的单号前缀；likePrefix 多了 LIKE 通配符 %，只用于查询，不能进单号
        String base = prefix + LocalDate.now().format(DATE_FMT);
        int seq = docNoMapper.selectMaxSeq(target.table(), target.column(),
                target.digits(), base + "%") + 1;
        return base + String.format("%0" + target.digits() + "d", seq);
    }

    private record Target(String table, String column, int digits) {
    }
}
