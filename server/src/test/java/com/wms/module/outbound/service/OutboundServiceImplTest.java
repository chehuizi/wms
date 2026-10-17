package com.wms.module.outbound.service;

import com.wms.common.BizException;
import com.wms.entity.Allocation;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundItem;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Sku;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.PickTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.mapper.ShipmentMapper;
import com.wms.mapper.SkuMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.vo.OutboundOrderDetailVO;
import com.wms.vo.OutboundOrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出库分配的业务规则测试（契约第 16.2 条）：
 * FIFO 取库存、全成或全败、可用量不足返回 40903。
 */
@ExtendWith(MockitoExtension.class)
class OutboundServiceImplTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private PickTaskMapper pickTaskMapper;
    @Mock
    private ShipmentMapper shipmentMapper;
    @Mock
    private SkuMapper skuMapper;
    @Mock
    private SerialMapper serialMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private StockService stockService;
    @Mock
    private DocNoGenerator docNoGenerator;
    @InjectMocks
    private OutboundServiceImpl outboundService;

    private OutboundOrder order(int status) {
        OutboundOrder o = new OutboundOrder();
        o.setId(10L);
        o.setOrderNo("OUT202610170001");
        o.setWarehouseId(1L);
        o.setStatus(status);
        return o;
    }

    private OutboundItem item(long skuId, String ordered, String allocated) {
        OutboundItem i = new OutboundItem();
        i.setId(100L);
        i.setOutboundId(10L);
        i.setSkuId(skuId);
        i.setOrderedQty(new BigDecimal(ordered));
        i.setAllocatedQty(new BigDecimal(allocated));
        i.setPickedQty(BigDecimal.ZERO);
        i.setShippedQty(BigDecimal.ZERO);
        return i;
    }

    private Inventory stockRow(long id, long locationId, String onHand, String locked,
                               String expireDate, long producedDate) {
        Inventory inv = new Inventory();
        inv.setId(id);
        inv.setWarehouseId(1L);
        inv.setLocationId(locationId);
        inv.setSkuId(1L);
        inv.setOnHandQty(new BigDecimal(onHand));
        inv.setLockedQty(new BigDecimal(locked));
        inv.setExpireDate(expireDate == null ? null : java.time.LocalDate.parse(expireDate));
        inv.setProducedDate(java.time.LocalDate.of(2026, 1, 1).plusDays(producedDate));
        return inv;
    }

    private void stubDetail() {
        OutboundOrderVO vo = new OutboundOrderVO();
        vo.setId(10L);
        vo.setStatus(3);
        when(outboundOrderMapper.selectVOById(10L)).thenReturn(vo);
        when(outboundOrderMapper.selectItemVOs(10L)).thenReturn(List.of());
        when(outboundOrderMapper.selectAllocationVOs(10L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("可用量不足返回 40903，且一条锁定与流水都不能留下")
    void shouldFailAllocationWithoutSideEffectsWhenStockNotEnough() {
        OutboundOrder o = order(2);
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(o);
        when(outboundOrderMapper.selectItems(10L)).thenReturn(List.of(item(1L, "100", "0")));

        Sku sku = new Sku();
        sku.setId(1L);
        sku.setCode("SKU-1001");
        when(skuMapper.selectById(1L)).thenReturn(sku);

        // 只有 40 可用，需求 100
        when(inventoryMapper.selectAvailableForSkuFifo(1L, 1L))
                .thenReturn(List.of(stockRow(1L, 7L, "40", "0", null, 0)));

        assertThatThrownBy(() -> outboundService.allocate(10L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40903);
                    assertThat(biz.getHttpStatus()).isEqualTo(409);
                    // message 必须写清缺多少
                    assertThat(biz.getMessage()).contains("SKU-1001").contains("需 100").contains("缺 60");
                });

        // 全成或全败：不能留下任何已锁定部分
        verify(stockService, never()).changeLocked(any(), any(), anyInt(), any(), any(), any());
        verify(outboundOrderMapper, never()).insertAllocation(any());
        verify(outboundOrderMapper, never()).addAllocatedQty(anyLong(), any());
        verify(outboundOrderMapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("多条明细中任意一条不足，整单都不分配")
    void shouldRollbackWholeOrderWhenAnyItemShort() {
        OutboundOrder o = order(2);
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(o);
        when(outboundOrderMapper.selectItems(10L)).thenReturn(List.of(item(1L, "10", "0")));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001"));

        // 第一条 SKU 可用充足，锁定会先发生；第二条 SKU 可用不足 -> 整单失败
        when(outboundOrderMapper.selectItems(10L)).thenReturn(List.of(
                item(1L, "10", "0"),
                item(2L, "10", "0")));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001"));
        when(skuMapper.selectById(2L)).thenReturn(sku(2L, "SKU-1002"));

        Inventory inv1 = stockRow(1L, 7L, "10", "0", null, 0);
        inv1.setSkuId(1L);
        Inventory inv2 = stockRow(2L, 8L, "5", "0", null, 0);
        inv2.setSkuId(2L);
        when(inventoryMapper.selectAvailableForSkuFifo(1L, 1L)).thenReturn(List.of(inv1));
        when(inventoryMapper.selectAvailableForSkuFifo(1L, 2L)).thenReturn(List.of(inv2));

        assertThatThrownBy(() -> outboundService.allocate(10L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40903))
                .satisfies(e -> assertThat(e.getMessage()).contains("SKU-1002"));

        // 第一条 SKU 的锁定靠异常回滚，数据库里不会留下痕迹
        verify(outboundOrderMapper, never()).insertAllocation(any());
        verify(outboundOrderMapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("库存充足时按 FIFO 切分行并逐行写锁定流水，订单转已分配")
    void shouldAllocateFifoAndLockEachRow() {
        OutboundOrder o = order(2);
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(o);
        when(outboundOrderMapper.selectItems(10L)).thenReturn(List.of(item(1L, "12", "0")));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001"));
        stubDetail();

        // FIFO 顺序由 SQL 保证，service 按返回顺序消费
        Inventory first = stockRow(1L, 7L, "5", "0", "2026-12-01", 0);
        Inventory second = stockRow(2L, 8L, "20", "0", null, 0);
        when(inventoryMapper.selectAvailableForSkuFifo(1L, 1L)).thenReturn(List.of(first, second));

        OutboundOrderDetailVO result = outboundService.allocate(10L);

        // 第一行取 5，第二行取 7
        ArgumentCaptor<Allocation> captor = ArgumentCaptor.forClass(Allocation.class);
        verify(outboundOrderMapper, times(2)).insertAllocation(captor.capture());
        List<Allocation> allocations = captor.getAllValues();
        assertThat(allocations).allSatisfy(a -> {
            assertThat(a.getOutboundId()).isEqualTo(10L);
            assertThat(a.getStatus()).isEqualTo(1);
        });
        assertThat(allocations.get(0).getAllocatedQty()).isEqualByComparingTo("5");
        assertThat(allocations.get(0).getInventoryId()).isEqualTo(1L);
        assertThat(allocations.get(1).getAllocatedQty()).isEqualByComparingTo("7");
        assertThat(allocations.get(1).getInventoryId()).isEqualTo(2L);

        verify(stockService).changeLocked(org.mockito.ArgumentMatchers.eq(first),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("5")),
                org.mockito.ArgumentMatchers.eq(MovementType.LOCK),
                org.mockito.ArgumentMatchers.any(BizRef.class),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any());
        verify(outboundOrderMapper).addAllocatedQty(eq(100L), eq(new BigDecimal("5")));
        verify(outboundOrderMapper).addAllocatedQty(eq(100L), eq(new BigDecimal("7")));
        verify(outboundOrderMapper).updateStatus(eq(10L), eq(3));
        assertThat(result.getStatus()).isEqualTo(3);
    }

    @Test
    @DisplayName("非待分配状态调用分配返回 40905")
    void shouldRejectAllocateOnWrongStatus() {
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(order(1));

        assertThatThrownBy(() -> outboundService.allocate(10L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40905));

        verify(inventoryMapper, never()).selectAvailableForSkuFifo(any(), any());
    }

    @Test
    @DisplayName("已发货订单不能取消，返回 40905 且不释放锁定")
    void shouldRejectCancelShippedOrder() {
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(order(6));

        assertThatThrownBy(() -> outboundService.cancel(10L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40905));

        verify(stockService, never()).changeLocked(any(), any(), anyInt(), any(), any(), any());
        verify(outboundOrderMapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("释放锁定按 allocatedQty - pickedQty 解锁并写 8 号流水")
    void shouldReleaseOnlyUnpickedPortion() {
        OutboundOrder o = order(3);
        when(outboundOrderMapper.selectForUpdate(10L)).thenReturn(o);
        stubDetail();

        Allocation alloc = new Allocation();
        alloc.setId(50L);
        alloc.setOutboundId(10L);
        alloc.setOutboundItemId(100L);
        alloc.setInventoryId(1L);
        alloc.setAllocatedQty(new BigDecimal("10"));
        alloc.setPickedQty(new BigDecimal("4"));
        alloc.setStatus(2);
        when(outboundOrderMapper.selectAllocationsByStatus(10L, List.of(1, 2))).thenReturn(List.of(alloc));

        Inventory inv = stockRow(1L, 7L, "10", "10", null, 0);
        when(inventoryMapper.selectByIdForUpdate(1L)).thenReturn(inv);

        outboundService.releaseAllocation(10L);

        // 只解锁 10 - 4 = 6
        verify(stockService).changeLocked(org.mockito.ArgumentMatchers.eq(inv),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("-6")),
                org.mockito.ArgumentMatchers.eq(MovementType.UNLOCK),
                org.mockito.ArgumentMatchers.any(BizRef.class),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any());
        verify(outboundOrderMapper).addAllocatedQty(eq(100L), eq(new BigDecimal("-6")));
        verify(outboundOrderMapper).addPickedQty(eq(100L), eq(new BigDecimal("-4")));
        verify(outboundOrderMapper).markAllocationStatus(50L, 5);
        verify(pickTaskMapper).cancelUnfinishedByAllocation(50L);
        // 释放后订单回到待分配
        verify(outboundOrderMapper).updateStatus(10L, 2);
    }

    @Test
    @DisplayName("新建出库单同一 SKU 重复出现返回 40001")
    void shouldRejectDuplicateSkuInItems() {
        com.wms.dto.OutboundOrderSaveRequest request = new com.wms.dto.OutboundOrderSaveRequest();
        com.wms.dto.OutboundOrderSaveRequest.Item a = new com.wms.dto.OutboundOrderSaveRequest.Item();
        a.setSkuId(1L);
        a.setOrderedQty(new BigDecimal("1"));
        com.wms.dto.OutboundOrderSaveRequest.Item b = new com.wms.dto.OutboundOrderSaveRequest.Item();
        b.setSkuId(1L);
        b.setOrderedQty(new BigDecimal("2"));
        request.setWarehouseId(1L);
        request.setItems(List.of(a, b));
        com.wms.entity.Warehouse warehouse = new com.wms.entity.Warehouse();
        warehouse.setId(1L);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        assertThatThrownBy(() -> outboundService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));
    }

    private Sku sku(long id, String code) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setCode(code);
        return sku;
    }
}
