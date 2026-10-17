package com.wms.module.outbound.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.PickRequest;
import com.wms.dto.PickTaskQuery;
import com.wms.entity.Allocation;
import com.wms.entity.PickTask;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.PickTaskMapper;
import com.wms.mapper.WaveMapper;
import com.wms.module.outbound.PickTaskService;
import com.wms.vo.PickTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拣货任务实现，见契约第 16.4 节。
 */
@Service
public class PickTaskServiceImpl implements PickTaskService {

    /** 任务状态：1=待拣货 2=拣货中 3=已完成 4=已取消 */
    private static final int TASK_PENDING = 1;
    private static final int TASK_PICKING = 2;
    private static final int TASK_DONE = 3;

    /** 分配状态：1=已锁定 2=拣货中 3=已拣货 */
    private static final int ALLOC_LOCKED = 1;
    private static final int ALLOC_PICKING = 2;
    private static final int ALLOC_PICKED = 3;

    /** 出库订单：4=拣货中 5=待发货 */
    private static final int ORDER_PICKING = 4;
    private static final int ORDER_WAITING_SHIP = 5;

    private final PickTaskMapper pickTaskMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final WaveMapper waveMapper;

    public PickTaskServiceImpl(PickTaskMapper pickTaskMapper,
                               OutboundOrderMapper outboundOrderMapper,
                               WaveMapper waveMapper) {
        this.pickTaskMapper = pickTaskMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.waveMapper = waveMapper;
    }

    @Override
    public PageResult<PickTaskVO> page(PickTaskQuery query) {
        long total = pickTaskMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(pickTaskMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    @Transactional
    public PickTaskVO pick(Long id, PickRequest request) {
        PickTask task = pickTaskMapper.selectForUpdate(id);
        if (task == null) {
            throw BizException.notFound("拣货任务不存在: " + id);
        }
        if (task.getStatus() != null && task.getStatus() != TASK_PENDING && task.getStatus() != TASK_PICKING) {
            throw BizException.orderStatusNotAllowed("该拣货任务已结束，当前状态: " + task.getStatus());
        }
        BigDecimal alreadyPicked = nvl(task.getPickedQty());
        BigDecimal pickedQty = request.getPickedQty();
        if (pickedQty == null || pickedQty.signum() <= 0) {
            throw BizException.paramInvalid("拣货数量必须大于 0");
        }
        // 允许部分拣，但累计不能超过应收
        if (alreadyPicked.add(pickedQty).compareTo(task.getRequiredQty()) > 0) {
            throw BizException.paramInvalid("拣货数量超过应收数量：应收 " + task.getRequiredQty()
                    + "，已拣 " + alreadyPicked + "，本次 " + pickedQty);
        }

        Allocation allocation = outboundOrderMapper.selectAllocationForUpdate(task.getAllocationId());
        if (allocation == null) {
            throw BizException.relatedInvalid("拣货任务关联的分配记录不存在: " + task.getAllocationId());
        }

        pickTaskMapper.addPickedQty(id, pickedQty);
        outboundOrderMapper.addAllocationPickedQty(allocation.getId(), pickedQty);
        outboundOrderMapper.addPickedQty(allocation.getOutboundItemId(), pickedQty);

        boolean finished = alreadyPicked.add(pickedQty).compareTo(task.getRequiredQty()) == 0;
        pickTaskMapper.markStatus(id, finished ? TASK_DONE : TASK_PICKING, request.getOperator());
        if (finished) {
            outboundOrderMapper.markAllocationStatus(allocation.getId(), ALLOC_PICKED);
        } else if (allocation.getStatus() == null || allocation.getStatus() != ALLOC_PICKING) {
            outboundOrderMapper.markAllocationStatus(allocation.getId(), ALLOC_PICKING);
        }

        // 订单全部明细拣满 -> 待发货
        if (outboundOrderMapper.countUnpickedItems(task.getOutboundId()) == 0) {
            outboundOrderMapper.updateStatus(task.getOutboundId(), ORDER_WAITING_SHIP);
        } else if (allocation.getStatus() != null && allocation.getStatus() == ALLOC_LOCKED) {
            outboundOrderMapper.updateStatus(task.getOutboundId(), ORDER_PICKING);
        }

        // 波次全部任务完成 -> 波次已完成
        if (task.getWaveId() != null && waveMapper.countUnfinishedPickTasks(task.getWaveId()) == 0) {
            waveMapper.markFinished(task.getWaveId());
        }

        return requireVO(id);
    }

    @Override
    @Transactional
    public PickTaskVO cancel(Long id) {
        PickTask task = pickTaskMapper.selectForUpdate(id);
        if (task == null) {
            throw BizException.notFound("拣货任务不存在: " + id);
        }
        if (task.getStatus() != null && (task.getStatus() == TASK_DONE || task.getStatus() == 4)) {
            throw BizException.orderStatusNotAllowed("已结束的拣货任务不能取消，当前状态: " + task.getStatus());
        }
        pickTaskMapper.markStatus(id, 4, null);
        if (task.getWaveId() != null && waveMapper.countUnfinishedPickTasks(task.getWaveId()) == 0) {
            waveMapper.markFinished(task.getWaveId());
        }
        return requireVO(id);
    }

    private PickTaskVO requireVO(Long id) {
        PickTaskVO vo = pickTaskMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("拣货任务不存在: " + id);
        }
        return vo;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
