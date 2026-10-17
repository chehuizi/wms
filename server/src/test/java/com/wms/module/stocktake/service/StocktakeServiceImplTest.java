package com.wms.module.stocktake.service;

import com.wms.common.BizException;
import com.wms.dto.StocktakeApproveRequest;
import com.wms.dto.StocktakeCountRequest;
import com.wms.dto.StocktakeCreateRequest;
import com.wms.entity.Inventory;
import com.wms.entity.StocktakeAdjust;
import com.wms.entity.StocktakeItem;
import com.wms.entity.StocktakeOrder;
import com.wms.entity.Warehouse;
import com.wms.mapper.InventoryMapper;
import com.wms.mapper.StocktakeMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
import com.wms.vo.StocktakeItemVO;
import com.wms.vo.StocktakeOrderVO;
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
 * 盘点的业务规则测试（契约第 17 节）：
 * 未录全拒绝结束、盘盈写 3 号流水、盘亏写 4 号流水、无差异不写流水。
 */
@ExtendWith(MockitoExtension.class)
class StocktakeServiceImplTest {

    @Mock
    private StocktakeMapper stocktakeMapper;
    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private StockService stockService;
    @Mock
    private DocNoGenerator docNoGenerator;
    @InjectMocks
    private StocktakeServiceImpl stocktakeService;

    private StocktakeOrder order(int status) {
        StocktakeOrder o = new StocktakeOrder();
        o.setId(1L);
        o.setOrderNo("ST202610170001");
        o.setWarehouseId(1L);
        o.setStocktakeType(1);
        o.setStatus(status);
        return o;
    }

    private StocktakeItem item(long id, long inventoryId, String bookQty, String diffQty) {
        StocktakeItem i = new StocktakeItem();
        i.setId(id);
        i.setStocktakeId(1L);
        i.setLocationId(7L);
        i.setSkuId(1L);
        i.setInventoryId(inventoryId);
        i.setBookQty(new BigDecimal(bookQty));
        i.setDiffQty(diffQty == null ? null : new BigDecimal(diffQty));
        i.setStatus(3);
        return i;
    }

    private Inventory inventory(long id) {
        Inventory inv = new Inventory();
        inv.setId(id);
        inv.setWarehouseId(1L);
        inv.setLocationId(7L);
        inv.setSkuId(1L);
        inv.setOnHandQty(new BigDecimal("100"));
        inv.setLockedQty(BigDecimal.ZERO);
        return inv;
    }

    private StocktakeOrderVO detail(int status) {
        StocktakeOrderVO vo = new StocktakeOrderVO();
        vo.setId(1L);
        vo.setStatus(status);
        return vo;
    }

    @Test
    @DisplayName("明细未录全就结束盘点返回 40005")
    void shouldRejectFinishWhenItemsUncounted() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(stocktakeMapper.countUncountedItems(1L)).thenReturn(2);

        assertThatThrownBy(() -> stocktakeService.finish(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40005);
                    assertThat(biz.getHttpStatus()).isEqualTo(400);
                });

        verify(stocktakeMapper, never()).markFinished(anyLong());
        verify(stocktakeMapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("盘盈按 +diff 调整库存并写 3 号流水")
    void shouldAdjustStockGainWithType3Movement() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(3));
        when(stocktakeMapper.selectItemEntities(1L)).thenReturn(List.of(item(10L, 100L, "100", "8")));
        when(inventoryMapper.selectByIdForUpdate(100L)).thenReturn(inventory(100L));
        when(stocktakeMapper.selectVOById(1L)).thenReturn(detail(4));
        when(stocktakeMapper.selectItems(1L)).thenReturn(List.of());
        when(stocktakeMapper.selectAdjusts(1L)).thenReturn(List.of());

        StocktakeApproveRequest request = new StocktakeApproveRequest();
        request.setApprover("周八");
        request.setRemark("差异已核实");

        stocktakeService.approve(1L, request);

        verify(stockService).changeOnHand(any(), eq(new BigDecimal("8")),
                eq(MovementType.STOCK_GAIN), any(BizRef.class), eq("周八"), eq("差异已核实"));

        ArgumentCaptor<StocktakeAdjust> captor = ArgumentCaptor.forClass(StocktakeAdjust.class);
        verify(stocktakeMapper).insertAdjust(captor.capture());
        assertThat(captor.getValue().getDiffQty()).isEqualByComparingTo("8");
        assertThat(captor.getValue().getInventoryId()).isEqualTo(100L);
        assertThat(captor.getValue().getApprovedBy()).isEqualTo("周八");

        verify(stocktakeMapper).markItemStatus(10L, 4);
        verify(stocktakeMapper).markApproved(1L, "周八");
    }

    @Test
    @DisplayName("盘亏按负数调整库存并写 4 号流水")
    void shouldAdjustStockLossWithType4Movement() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(3));
        when(stocktakeMapper.selectItemEntities(1L)).thenReturn(List.of(item(10L, 100L, "100", "-12")));
        when(inventoryMapper.selectByIdForUpdate(100L)).thenReturn(inventory(100L));
        when(stocktakeMapper.selectVOById(1L)).thenReturn(detail(4));
        when(stocktakeMapper.selectItems(1L)).thenReturn(List.of());
        when(stocktakeMapper.selectAdjusts(1L)).thenReturn(List.of());

        StocktakeApproveRequest request = new StocktakeApproveRequest();
        request.setApprover("周八");

        stocktakeService.approve(1L, request);

        // 负数即扣减，类型为 4 盘亏
        verify(stockService).changeOnHand(any(), eq(new BigDecimal("-12")),
                eq(MovementType.STOCK_LOSS), any(BizRef.class), eq("周八"), eq((String) null));
        verify(stocktakeMapper).markItemStatus(10L, 4);
    }

    @Test
    @DisplayName("无差异明细不写流水，但状态仍置为已调账")
    void shouldNotWriteMovementForZeroDiff() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(3));
        when(stocktakeMapper.selectItemEntities(1L))
                .thenReturn(List.of(item(10L, 100L, "50", "0"), item(11L, 101L, "30", "2")));
        when(inventoryMapper.selectByIdForUpdate(101L)).thenReturn(inventory(101L));
        when(stocktakeMapper.selectVOById(1L)).thenReturn(detail(4));
        when(stocktakeMapper.selectItems(1L)).thenReturn(List.of());
        when(stocktakeMapper.selectAdjusts(1L)).thenReturn(List.of());

        StocktakeApproveRequest request = new StocktakeApproveRequest();
        request.setApprover("周八");

        stocktakeService.approve(1L, request);

        // 只对有差异的那条写流水
        verify(stockService, times(1)).changeOnHand(any(), any(), anyInt(), any(), any(), any());
        verify(stocktakeMapper, times(1)).insertAdjust(any());
        verify(stocktakeMapper, never()).insertAdjust(argThatDiffIsZero());
        // 两条明细都置为已调账
        verify(stocktakeMapper).markItemStatus(10L, 4);
        verify(stocktakeMapper).markItemStatus(11L, 4);
    }

    private StocktakeAdjust argThatDiffIsZero() {
        return org.mockito.ArgumentMatchers.argThat(adjust -> adjust.getDiffQty().signum() == 0);
    }

    @Test
    @DisplayName("录入实盘计算差异 = 实盘 - 账面，0 也要显式录入")
    void shouldComputeDiffAsCountedMinusBook() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(stocktakeMapper.selectItemForUpdate(10L)).thenReturn(item(10L, 100L, "100", null));
        when(stocktakeMapper.selectVOById(1L)).thenReturn(detail(2));
        when(stocktakeMapper.selectItems(1L)).thenReturn(List.of());
        when(stocktakeMapper.selectAdjusts(1L)).thenReturn(List.of());

        StocktakeCountRequest request = new StocktakeCountRequest();
        request.setOperator("李四");
        StocktakeCountRequest.Item line = new StocktakeCountRequest.Item();
        line.setItemId(10L);
        line.setCountedQty(new BigDecimal("98"));
        line.setRemark("少 2 箱");
        request.setItems(List.of(line));

        stocktakeService.count(1L, request);

        // 98 - 100 = -2
        verify(stocktakeMapper).updateCounted(eq(10L), eq(new BigDecimal("98")), eq(new BigDecimal("-2")),
                eq("李四"), eq("少 2 箱"));
        verify(stocktakeMapper).markAllItemsStatus(1L, 2);
        verify(stocktakeMapper).refreshCountedLocations(1L);
    }

    @Test
    @DisplayName("草稿或进行中之外的状态不能开始盘点，返回 40905")
    void shouldRejectStartOnWrongStatus() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(3));

        assertThatThrownBy(() -> stocktakeService.start(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40905));

        verify(stocktakeMapper, never()).markStarted(anyLong());
    }

    @Test
    @DisplayName("无差异时结束盘点直接转已完成")
    void shouldFinishDirectlyWhenNoDiff() {
        when(stocktakeMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(stocktakeMapper.countUncountedItems(1L)).thenReturn(0);
        when(stocktakeMapper.countDiffItems(1L)).thenReturn(0);
        when(stocktakeMapper.selectVOById(1L)).thenReturn(detail(4));
        when(stocktakeMapper.selectItems(1L)).thenReturn(List.of());
        when(stocktakeMapper.selectAdjusts(1L)).thenReturn(List.of());

        stocktakeService.finish(1L);

        verify(stocktakeMapper).updateStatus(1L, 4);
    }

    @Test
    @DisplayName("循环盘/抽盘必须指定库区，否则 40001")
    void shouldRequireScopeZoneForPartialStocktake() {
        StocktakeCreateRequest request = new StocktakeCreateRequest();
        request.setWarehouseId(1L);
        request.setStocktakeType(2);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        assertThatThrownBy(() -> stocktakeService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));

        verify(stocktakeMapper, never()).insert(any());
    }

    @Test
    @DisplayName("全盘创建时按范围内现有库存生成明细并回写库位数")
    void shouldGenerateItemsOnCreate() {
        StocktakeCreateRequest request = new StocktakeCreateRequest();
        request.setWarehouseId(1L);
        request.setStocktakeType(1);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        when(docNoGenerator.next(DocNoGenerator.STOCKTAKE_ORDER)).thenReturn("ST202610170001");
        when(stocktakeMapper.selectVOById(any())).thenReturn(detail(1));

        StocktakeItemVO a = new StocktakeItemVO();
        a.setLocationId(7L);
        StocktakeItemVO b = new StocktakeItemVO();
        b.setLocationId(8L);
        StocktakeItemVO c = new StocktakeItemVO();
        c.setLocationId(7L);
        when(stocktakeMapper.selectItems(any())).thenReturn(List.of(a, b, c));
        when(stocktakeMapper.selectAdjusts(any())).thenReturn(List.of());

        stocktakeService.create(request);

        verify(stocktakeMapper).insertItemsFromInventory(any(), eq(1L), eq((String) null));
        // 3 条明细落在 2 个库位
        verify(stocktakeMapper).setTotalLocations(any(), eq(2));
    }
}
