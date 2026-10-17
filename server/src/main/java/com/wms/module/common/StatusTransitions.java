package com.wms.module.common;

import com.wms.common.BizException;

import java.util.Map;
import java.util.Set;

/**
 * 单据状态机白名单，见契约第 12 节。
 * 任何不在表里的「当前状态 -> 目标状态」都直接按 40004 拒绝，
 * 避免出现「已取消的单还能收货」这类脏流转。
 */
public final class StatusTransitions {

    /** 入库到货单：1=草稿 2=待收货 3=部分收货 4=已完成 5=已取消 */
    private static final Map<Integer, Set<Integer>> INBOUND = Map.of(
            1, Set.of(2, 5),
            2, Set.of(3, 4, 5),
            3, Set.of(3, 4, 5),
            4, Set.of(),
            5, Set.of()
    );

    /** 出库订单：1=草稿 2=待分配 3=已分配 4=拣货中 5=待发货 6=已发货 7=已取消 */
    private static final Map<Integer, Set<Integer>> OUTBOUND = Map.of(
            1, Set.of(2, 7),
            2, Set.of(3, 7),
            3, Set.of(4, 7),
            4, Set.of(5, 7),
            5, Set.of(6, 7),
            6, Set.of(),
            7, Set.of()
    );

    /** 盘点单：1=草稿 2=进行中 3=待审批 4=已完成 5=已取消 */
    private static final Map<Integer, Set<Integer>> STOCKTAKE = Map.of(
            1, Set.of(2, 5),
            2, Set.of(3, 5),
            3, Set.of(4),
            4, Set.of(),
            5, Set.of()
    );

    private StatusTransitions() {
    }

    public static void requireInbound(Integer current, Integer target, String orderNo) {
        require(INBOUND, current, target, "入库到货单", orderNo);
    }

    public static void requireOutbound(Integer current, Integer target, String orderNo) {
        require(OUTBOUND, current, target, "出库订单", orderNo);
    }

    public static void requireStocktake(Integer current, Integer target, String orderNo) {
        require(STOCKTAKE, current, target, "盘点单", orderNo);
    }

    private static void require(Map<Integer, Set<Integer>> table, Integer current, Integer target,
                                String docName, String docNo) {
        Set<Integer> allowed = table.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw BizException.statusInvalid(
                    docName + " " + docNo + " 当前状态 " + current + " 不允许变更为 " + target);
        }
    }
}
