package com.wms.module.outbound.service;

import com.wms.common.BizException;
import com.wms.dto.PickRequest;
import com.wms.entity.Allocation;
import com.wms.entity.PickTask;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.PickTaskMapper;
import com.wms.mapper.WaveMapper;
import com.wms.vo.PickTaskVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拣货的业务规则测试（契约第 16.4 节）。
 */
@ExtendWith(MockitoExtension.class)
class PickTaskServiceImplTest {

    @Mock
    private PickTaskMapper pickTaskMapper;
    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private WaveMapper waveMapper;
    @InjectMocks
    private PickTaskServiceImpl pickTaskService;

    private PickTask task(int status, String required, String picked) {
        PickTask t = new PickTask();
        t.setId(60L);
        t.setOutboundId(10L);
        t.setAllocationId(50L);
        t.setRequiredQty(new BigDecimal(required));
        t.setPickedQty(new BigDecimal(picked));
        t.setStatus(status);
        return t;
    }

    private Allocation allocation() {
        Allocation a = new Allocation();
        a.setId(50L);
        a.setOutboundItemId(100L);
        a.setStatus(2);
        return a;
    }

    private PickRequest request(String qty) {
        PickRequest r = new PickRequest();
        r.setPickedQty(new BigDecimal(qty));
        r.setOperator("赵六");
        return r;
    }

    @Test
    @DisplayName("拣满后任务与分配转已拣货，订单转待发货")
    void shouldCompleteTaskAndAdvanceOrder() {
        when(pickTaskMapper.selectForUpdate(60L)).thenReturn(task(1, "10", "0"));
        when(outboundOrderMapper.selectAllocationForUpdate(50L)).thenReturn(allocation());
        when(outboundOrderMapper.countUnpickedItems(10L)).thenReturn(0);
        when(pickTaskMapper.selectVOById(60L)).thenReturn(new PickTaskVO());

        pickTaskService.pick(60L, request("10"));

        verify(pickTaskMapper).addPickedQty(60L, new BigDecimal("10"));
        verify(outboundOrderMapper).addAllocationPickedQty(50L, new BigDecimal("10"));
        verify(outboundOrderMapper).addPickedQty(100L, new BigDecimal("10"));
        verify(pickTaskMapper).markStatus(60L, 3, "赵六");
        verify(outboundOrderMapper).markAllocationStatus(50L, 3);
        // 订单全部拣满 -> 待发货
        verify(outboundOrderMapper).updateStatus(10L, 5);
    }

    @Test
    @DisplayName("部分拣货时任务保持拣货中，订单维持拣货中")
    void shouldAllowPartialPick() {
        when(pickTaskMapper.selectForUpdate(60L)).thenReturn(task(1, "10", "0"));
        when(outboundOrderMapper.selectAllocationForUpdate(50L)).thenReturn(allocation());
        when(outboundOrderMapper.countUnpickedItems(10L)).thenReturn(1);
        when(pickTaskMapper.selectVOById(60L)).thenReturn(new PickTaskVO());

        pickTaskService.pick(60L, request("4"));

        verify(pickTaskMapper).markStatus(60L, 2, "赵六");
        verify(outboundOrderMapper, never()).markAllocationStatus(50L, 3);
        verify(outboundOrderMapper, never()).updateStatus(10L, 5);
    }

    @Test
    @DisplayName("超过应收数量时拒绝，返回 40001")
    void shouldRejectOverPick() {
        when(pickTaskMapper.selectForUpdate(60L)).thenReturn(task(1, "10", "0"));

        assertThatThrownBy(() -> pickTaskService.pick(60L, request("11")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));

        verify(pickTaskMapper, never()).addPickedQty(any(), any());
        verify(outboundOrderMapper, never()).addPickedQty(any(), any());
    }

    @Test
    @DisplayName("已完成的拣货任务不能再次拣货，返回 40905")
    void shouldRejectPickOnFinishedTask() {
        when(pickTaskMapper.selectForUpdate(60L)).thenReturn(task(3, "10", "10"));

        assertThatThrownBy(() -> pickTaskService.pick(60L, request("1")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40905));

        verify(pickTaskMapper, never()).addPickedQty(any(), any());
    }

    @Test
    @DisplayName("波次内全部任务完成后波次转已完成")
    void shouldFinishWaveWhenAllTasksDone() {
        PickTask t = task(1, "10", "0");
        t.setWaveId(70L);
        when(pickTaskMapper.selectForUpdate(60L)).thenReturn(t);
        when(outboundOrderMapper.selectAllocationForUpdate(50L)).thenReturn(allocation());
        when(outboundOrderMapper.countUnpickedItems(10L)).thenReturn(0);
        when(waveMapper.countUnfinishedPickTasks(70L)).thenReturn(0);
        when(pickTaskMapper.selectVOById(60L)).thenReturn(new PickTaskVO());

        pickTaskService.pick(60L, request("10"));

        verify(waveMapper).markFinished(70L);
    }
}
