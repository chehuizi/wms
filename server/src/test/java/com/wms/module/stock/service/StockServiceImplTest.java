package com.wms.module.stock.service;

import com.wms.common.BizException;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.entity.StockMovement;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.StockMovementMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存核心不变式的单元测试（契约第 10 节）：
 * 有账必有流水、锁定量不超在库量、禁止负库存、移库产生两条流水。
 */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private StockMovementMapper movementMapper;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private DocNoGenerator docNoGenerator;
    @InjectMocks
    private StockServiceImpl stockService;

    private Inventory inventory(long onHand, long locked) {
        Inventory inv = new Inventory();
        inv.setId(1L);
        inv.setWarehouseId(1L);
        inv.setLocationId(7L);
        inv.setSkuId(1L);
        inv.setBatchNo(null);
        inv.setSerialNo(null);
        inv.setOnHandQty(new BigDecimal(onHand));
        inv.setLockedQty(new BigDecimal(locked));
        return inv;
    }

    private void stubDocNo() {
        when(docNoGenerator.next(DocNoGenerator.MOVEMENT)).thenReturn("MV2026101700001");
    }

    @Test
    @DisplayName("改在库量后同事务写一条流水，并带 before/after 快照")
    void shouldWriteMovementWithSnapshot() {
        stubDocNo();
        Inventory inv = inventory(100, 0);

        stockService.changeOnHand(inv, new BigDecimal("10"), MovementType.INBOUND_RECEIPT,
                BizRef.of("INBOUND", "IN202610170001"), "张三", "收货");

        verify(inventoryMapper).updateQty(1L, new BigDecimal("110"), BigDecimal.ZERO);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementMapper).insert(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.INBOUND_RECEIPT);
        assertThat(movement.getQtyChange()).isEqualByComparingTo("10");
        assertThat(movement.getLockedChange()).isEqualByComparingTo("0");
        assertThat(movement.getBeforeQty()).isEqualByComparingTo("100");
        assertThat(movement.getAfterQty()).isEqualByComparingTo("110");
        assertThat(movement.getBizNo()).isEqualTo("IN202610170001");
    }

    @Test
    @DisplayName("移库必须先写 5 移库出再写 6 移库入，且顺序固定")
    void shouldWriteTwoMovementsInOrderForMove() {
        stubDocNo();
        Inventory from = inventory(50, 0);
        from.setId(1L);
        from.setLocationId(12L);
        Inventory to = inventory(0, 0);
        to.setId(2L);
        to.setLocationId(7L);
        when(inventoryMapper.selectByIdForUpdate(1L)).thenReturn(from);
        when(inventoryMapper.selectByIdForUpdate(2L)).thenReturn(to);

        stockService.moveStock(1L, 2L, new BigDecimal("20"),
                BizRef.of("INBOUND", "IN202610170001"), "李四", "上架");

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementMapper, times(2)).insert(captor.capture());
        List<StockMovement> movements = captor.getAllValues();
        assertThat(movements).hasSize(2);
        // 不变式 7：先 5 出后 6 入
        assertThat(movements.get(0).getMovementType()).isEqualTo(MovementType.MOVE_OUT);
        assertThat(movements.get(0).getQtyChange()).isEqualByComparingTo("-20");
        assertThat(movements.get(0).getLocationId()).isEqualTo(12L);
        assertThat(movements.get(0).getBeforeQty()).isEqualByComparingTo("50");
        assertThat(movements.get(0).getAfterQty()).isEqualByComparingTo("30");
        assertThat(movements.get(1).getMovementType()).isEqualTo(MovementType.MOVE_IN);
        assertThat(movements.get(1).getQtyChange()).isEqualByComparingTo("20");
        assertThat(movements.get(1).getLocationId()).isEqualTo(7L);
        assertThat(movements.get(1).getBeforeQty()).isEqualByComparingTo("0");
        assertThat(movements.get(1).getAfterQty()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("扣成负库存时返回 40904，且不写库存也不写流水")
    void shouldRejectNegativeStock() {
        Inventory inv = inventory(5, 0);

        assertThatThrownBy(() -> stockService.changeOnHand(inv, new BigDecimal("-10"),
                MovementType.OUTBOUND_SHIP, BizRef.of("OUTBOUND", "OUT1"), "赵六", null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40904);
                    assertThat(biz.getHttpStatus()).isEqualTo(409);
                });

        verify(inventoryMapper, never()).updateQty(anyLong(), any(), any());
        verify(movementMapper, never()).insert(any());
    }

    @Test
    @DisplayName("锁定量超过在库量时被拦截，不依赖 DB 约束")
    void shouldRejectLockedOverOnHand() {
        Inventory inv = inventory(10, 0);

        assertThatThrownBy(() -> stockService.changeLocked(inv, new BigDecimal("20"),
                MovementType.LOCK, BizRef.of("OUTBOUND", "OUT1"), null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("锁定量不得超过在库量");

        verify(inventoryMapper, never()).updateQty(anyLong(), any(), any());
        verify(movementMapper, never()).insert(any());
    }

    @Test
    @DisplayName("出库发货同时扣在库量与锁定量，只写一条流水")
    void shouldWriteSingleMovementWhenBothQuantitiesChange() {
        stubDocNo();
        Inventory inv = inventory(100, 40);

        stockService.change(inv, new BigDecimal("-10"), new BigDecimal("-10"),
                MovementType.OUTBOUND_SHIP, BizRef.of("OUTBOUND", "OUT202610170001"), "孙七", "发货");

        verify(inventoryMapper).updateQty(1L, new BigDecimal("90"), new BigDecimal("30"));
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getQtyChange()).isEqualByComparingTo("-10");
        assertThat(captor.getValue().getLockedChange()).isEqualByComparingTo("-10");
        assertThat(captor.getValue().getBeforeQty()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getAfterQty()).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("数量没有变化时不写流水")
    void shouldSkipMovementWhenNothingChanged() {
        Inventory inv = inventory(100, 40);

        stockService.change(inv, BigDecimal.ZERO, BigDecimal.ZERO,
                MovementType.MANUAL_ADJUST, BizRef.of("MANUAL", null), "张三", null);

        verify(movementMapper, never()).insert(any());
        verify(inventoryMapper, never()).updateQty(anyLong(), any(), any());
    }

    @Test
    @DisplayName("未加锁的库存行不允许变更")
    void shouldRejectUnlockedInventory() {
        assertThatThrownBy(() -> stockService.changeOnHand(new Inventory(), new BigDecimal("1"),
                MovementType.MANUAL_ADJUST, BizRef.of("MANUAL", null), "张三", null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));
    }

    @Test
    @DisplayName("lockOrCreate 命中已有行时直接返回，不新建")
    void shouldReuseExistingInventoryRow() {
        Inventory existing = inventory(30, 0);
        when(inventoryMapper.selectForUpdateByKey(7L, 1L, null, null)).thenReturn(existing);

        Inventory result = stockService.lockOrCreate(1L, 7L, 1L, null, null, null, null);

        assertThat(result).isSameAs(existing);
        verify(inventoryMapper, never()).insert(any());
    }
}
