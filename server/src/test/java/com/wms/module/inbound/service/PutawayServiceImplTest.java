package com.wms.module.inbound.service;

import com.wms.common.BizException;
import com.wms.dto.PutawayExecuteRequest;
import com.wms.entity.InboundOrder;
import com.wms.entity.Location;
import com.wms.entity.PutawayTask;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.PutawayTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.StockService;
import com.wms.vo.PutawayTaskVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 上架执行的业务规则测试（契约第 15.2 节）。
 */
@ExtendWith(MockitoExtension.class)
class PutawayServiceImplTest {

    @Mock
    private PutawayTaskMapper putawayTaskMapper;
    @Mock
    private InboundOrderMapper inboundOrderMapper;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private SerialMapper serialMapper;
    @Mock
    private StockService stockService;
    @InjectMocks
    private PutawayServiceImpl putawayService;

    /** 批次的生产/失效日期——**批次随货移动，效期也是批次的一部分** */
    private static final LocalDate PRODUCED = LocalDate.of(2026, 10, 1);
    private static final LocalDate EXPIRE = LocalDate.of(2027, 10, 1);

    private PutawayTask task(int status) {
        PutawayTask t = new PutawayTask();
        t.setId(40L);
        t.setInboundId(1L);
        t.setSkuId(1L);
        t.setFromLocationId(12L);
        t.setBatchNo("B1");
        t.setProducedDate(PRODUCED);
        t.setExpireDate(EXPIRE);
        t.setQty(new BigDecimal("30"));
        t.setStatus(status);
        return t;
    }

    private Location location(long id, int type) {
        Location l = new Location();
        l.setId(id);
        l.setWarehouseId(1L);
        l.setCode("L-" + id);
        l.setLocationType(type);
        l.setStatus(1);
        return l;
    }

    private InboundOrder inbound() {
        InboundOrder o = new InboundOrder();
        o.setId(1L);
        o.setOrderNo("IN202610170001");
        o.setWarehouseId(1L);
        o.setStatus(2);
        return o;
    }

    private com.wms.entity.Inventory inventory(long id, long locationId) {
        com.wms.entity.Inventory inv = new com.wms.entity.Inventory();
        inv.setId(id);
        inv.setWarehouseId(1L);
        inv.setLocationId(locationId);
        inv.setSkuId(1L);
        inv.setOnHandQty(BigDecimal.ZERO);
        inv.setLockedQty(BigDecimal.ZERO);
        return inv;
    }

    private PutawayExecuteRequest request(Long toLocationId) {
        PutawayExecuteRequest r = new PutawayExecuteRequest();
        r.setToLocationId(toLocationId);
        r.setOperator("李四");
        return r;
    }

    @Test
    @DisplayName("执行上架触发一次移库，任务转已完成")
    void shouldExecutePutawayAsMove() {
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(task(1));
        when(inboundOrderMapper.selectById(1L)).thenReturn(inbound());
        when(locationMapper.selectById(7L)).thenReturn(location(7L, 1));
        when(putawayTaskMapper.selectVOById(40L)).thenReturn(new PutawayTaskVO());
        when(stockService.lockOrCreate(eq(1L), eq(12L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE))).thenReturn(inventory(1L, 12L));
        when(stockService.lockOrCreate(eq(1L), eq(7L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE))).thenReturn(inventory(2L, 7L));

        putawayService.execute(40L, request(7L));

        verify(stockService).lockOrCreate(eq(1L), eq(12L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE));
        verify(stockService).lockOrCreate(eq(1L), eq(7L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE));
        // 移库由 StockService 统一写 5/6 两条流水
        verify(stockService).moveStock(any(), any(), eq(new BigDecimal("30")), any(BizRef.class),
                eq("李四"), any());
        verify(putawayTaskMapper).markCompleted(eq(40L), eq(7L), eq("李四"));
    }

    @Test
    @DisplayName("上架时批次的效期随货移动到目标库位")
    void shouldMoveBatchExpiryToTargetLocation() {
        // 回归测试：以前执行上架时给库存行传的是 (null, null)，
        // 于是货到了存储位效期就没了 —— FIFO 分配（ORDER BY expire_date）
        // 失效、临期预警恒为 0，而且**没有任何报错**。
        //
        // 这个 bug 躲过了 61 个单测，因为原来的测试**把 bug 写进了断言**：
        // 它自己就 stub 了 (null, null)。测试钉住的是错误行为。
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(task(1));
        when(inboundOrderMapper.selectById(1L)).thenReturn(inbound());
        when(locationMapper.selectById(7L)).thenReturn(location(7L, 1));
        when(putawayTaskMapper.selectVOById(40L)).thenReturn(new PutawayTaskVO());
        when(stockService.lockOrCreate(eq(1L), eq(12L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE))).thenReturn(inventory(1L, 12L));
        when(stockService.lockOrCreate(eq(1L), eq(7L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE))).thenReturn(inventory(2L, 7L));

        putawayService.execute(40L, request(7L));

        // 目标库位那行必须带效期——它是效期先出的依据
        verify(stockService).lockOrCreate(eq(1L), eq(7L), eq(1L), eq("B1"), eq((String) null),
                eq(PRODUCED), eq(EXPIRE));
    }

    @Test
    @DisplayName("已完成的上架任务不能重复执行，返回 40905")
    void shouldRejectExecutingCompletedTask() {
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(task(2));

        assertThatThrownBy(() -> putawayService.execute(40L, request(7L)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40905));

        verify(stockService, never()).moveStock(any(), any(), any(), any(), any(), any());
        verify(putawayTaskMapper, never()).markCompleted(any(), any(), any());
    }

    @Test
    @DisplayName("上架目标位必须是存储位，返回 40003")
    void shouldRejectNonStorageTargetLocation() {
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(task(1));
        when(inboundOrderMapper.selectById(1L)).thenReturn(inbound());
        // 收货暂存位不能作为上架目标
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 2));

        assertThatThrownBy(() -> putawayService.execute(40L, request(12L)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40003));

        verify(stockService, never()).moveStock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("目标库位与收货位相同时拒绝，返回 40001")
    void shouldRejectSameSourceAndTargetLocation() {
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(task(1));
        when(inboundOrderMapper.selectById(1L)).thenReturn(inbound());
        when(locationMapper.selectById(7L)).thenReturn(location(7L, 1));

        PutawayTask same = task(1);
        same.setFromLocationId(7L);
        when(putawayTaskMapper.selectForUpdate(40L)).thenReturn(same);

        assertThatThrownBy(() -> putawayService.execute(40L, request(7L)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));
    }
}
