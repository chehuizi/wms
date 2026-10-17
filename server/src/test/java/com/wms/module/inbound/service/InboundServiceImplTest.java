package com.wms.module.inbound.service;

import com.wms.common.BizException;
import com.wms.dto.ReceiptRequest;
import com.wms.entity.InboundItem;
import com.wms.entity.InboundOrder;
import com.wms.entity.Location;
import com.wms.entity.PutawayTask;
import com.wms.entity.InboundReceipt;
import com.wms.entity.Sku;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.PutawayTaskMapper;
import com.wms.mapper.SerialMapper;
import com.wms.mapper.SkuMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.stock.BizRef;
import com.wms.module.stock.MovementType;
import com.wms.module.stock.StockService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 入库收货与上架的业务规则测试（契约第 15 节）：
 * 超收拦截、收货状态机、收货落库存与写流水、上架产生两条流水。
 */
@ExtendWith(MockitoExtension.class)
class InboundServiceImplTest {

    @Mock
    private InboundOrderMapper inboundOrderMapper;
    @Mock
    private PutawayTaskMapper putawayTaskMapper;
    @Mock
    private SkuMapper skuMapper;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private SerialMapper serialMapper;
    @Mock
    private StockService stockService;
    @Mock
    private DocNoGenerator docNoGenerator;
    @InjectMocks
    private InboundServiceImpl inboundService;

    private InboundOrder order(int status) {
        InboundOrder o = new InboundOrder();
        o.setId(1L);
        o.setOrderNo("IN202610170001");
        o.setWarehouseId(1L);
        o.setStatus(status);
        return o;
    }

    private Location location(long id, long warehouseId, int type) {
        Location l = new Location();
        l.setId(id);
        l.setWarehouseId(warehouseId);
        l.setCode("L-" + id);
        l.setLocationType(type);
        l.setStatus(1);
        l.setAllowNegative(0);
        return l;
    }

    private Sku sku(long id, String code, int serialManaged) {
        Sku s = new Sku();
        s.setId(id);
        s.setCode(code);
        s.setSerialManaged(serialManaged);
        return s;
    }

    private ReceiptRequest receiptRequest(Long locationId, Long inboundItemId, String qty) {
        ReceiptRequest request = new ReceiptRequest();
        request.setLocationId(locationId);
        request.setOperator("张三");
        ReceiptRequest.Item item = new ReceiptRequest.Item();
        item.setInboundItemId(inboundItemId);
        item.setQty(new BigDecimal(qty));
        request.setItems(List.of(item));
        return request;
    }

    private InboundItem inboundItem(String expected, String received) {
        InboundItem item = new InboundItem();
        item.setId(20L);
        item.setInboundId(1L);
        item.setSkuId(1L);
        item.setExpectedQty(new BigDecimal(expected));
        item.setReceivedQty(new BigDecimal(received));
        return item;
    }

    @Test
    @DisplayName("超收返回 40903，且不落库存、不写流水、不生成上架任务")
    void shouldRejectOverReceipt() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 1L, 2));
        // 已收 90，应收 100，本次再收 20 -> 超收 10
        when(inboundOrderMapper.selectItemForUpdate(20L)).thenReturn(inboundItem("100", "90"));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001", 0));
        when(docNoGenerator.next(DocNoGenerator.RECEIPT)).thenReturn("RC202610170001");

        assertThatThrownBy(() -> inboundService.receipt(1L, receiptRequest(12L, 20L, "20")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40903);
                    assertThat(biz.getMessage()).contains("SKU-1001").contains("超收 10");
                });

        verify(stockService, never()).changeOnHand(any(), any(), anyInt(), any(), any(), any());
        verify(inboundOrderMapper, never()).addReceivedQty(anyLong(), any());
        verify(putawayTaskMapper, never()).insert(any());
    }

    @Test
    @DisplayName("已取消的到货单不能收货，返回 40004")
    void shouldRejectReceiptOnCancelledOrder() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(5));

        assertThatThrownBy(() -> inboundService.receipt(1L, receiptRequest(12L, 20L, "10")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40004));

        verify(stockService, never()).changeOnHand(any(), any(), anyInt(), any(), any(), any());
        verify(inboundOrderMapper, never()).insertReceipt(any());
    }

    @Test
    @DisplayName("已完成的到货单不能继续收货，返回 40004")
    void shouldRejectReceiptOnFinishedOrder() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(4));

        assertThatThrownBy(() -> inboundService.receipt(1L, receiptRequest(12L, 20L, "10")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40004));
    }

    @Test
    @DisplayName("收货位必须是本仓库的收货暂存位")
    void shouldRejectWrongLocationType() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        // 传了存储位（type=1）而不是收货暂存位（type=2）
        when(locationMapper.selectById(7L)).thenReturn(location(7L, 1L, 1));

        assertThatThrownBy(() -> inboundService.receipt(1L, receiptRequest(7L, 20L, "10")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40003));
    }

    @Test
    @DisplayName("收货写入在库量并生成上架任务")
    void shouldIncreaseStockAndCreatePutawayTask() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 1L, 2));
        when(inboundOrderMapper.selectItemForUpdate(20L)).thenReturn(inboundItem("100", "0"));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001", 0));
        when(docNoGenerator.next(DocNoGenerator.RECEIPT)).thenReturn("RC202610170001");
        when(docNoGenerator.next(DocNoGenerator.PUTAWAY_TASK)).thenReturn("PT202610170001");
        when(inboundOrderMapper.insertReceipt(any())).thenAnswer(inv -> {
            InboundReceipt r = inv.getArgument(0);
            r.setId(30L);
            return 1;
        });
        // 明细全部收满
        when(inboundOrderMapper.countUnfinishedItems(1L)).thenReturn(0);
        when(inboundOrderMapper.selectReceiptItems(30L)).thenReturn(List.of());

        inboundService.receipt(1L, receiptRequest(12L, 20L, "100"));

        verify(stockService).changeOnHand(any(), eq(new BigDecimal("100")),
                eq(MovementType.INBOUND_RECEIPT), any(BizRef.class), eq("张三"), any());
        verify(inboundOrderMapper).addReceivedQty(20L, new BigDecimal("100"));

        ArgumentCaptor<PutawayTask> captor = ArgumentCaptor.forClass(PutawayTask.class);
        verify(putawayTaskMapper).insert(captor.capture());
        PutawayTask task = captor.getValue();
        assertThat(task.getFromLocationId()).isEqualTo(12L);
        assertThat(task.getStatus()).isEqualTo(1);
        assertThat(task.getQty()).isEqualByComparingTo("100");
        assertThat(task.getTaskNo()).isEqualTo("PT202610170001");

        // 全部收满 -> 已完成
        verify(inboundOrderMapper).markFinished(eq(1L), any());
    }

    @Test
    @DisplayName("部分收货后到货单转部分收货")
    void shouldMarkPartialWhenNotAllReceived() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 1L, 2));
        when(inboundOrderMapper.selectItemForUpdate(20L)).thenReturn(inboundItem("100", "0"));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1001", 0));
        when(docNoGenerator.next(DocNoGenerator.RECEIPT)).thenReturn("RC202610170001");
        when(docNoGenerator.next(DocNoGenerator.PUTAWAY_TASK)).thenReturn("PT202610170001");
        when(inboundOrderMapper.insertReceipt(any())).thenAnswer(inv -> {
            InboundReceipt r = inv.getArgument(0);
            r.setId(30L);
            return 1;
        });
        when(inboundOrderMapper.countUnfinishedItems(1L)).thenReturn(1);
        when(inboundOrderMapper.selectReceiptItems(30L)).thenReturn(List.of());

        inboundService.receipt(1L, receiptRequest(12L, 20L, "40"));

        verify(inboundOrderMapper).updateStatus(1L, 3);
        verify(inboundOrderMapper, never()).markFinished(anyLong(), any());
    }

    @Test
    @DisplayName("开序列号管理的 SKU 收货必须带序列号")
    void shouldRequireSerialNoForSerialManagedSku() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 1L, 2));
        when(inboundOrderMapper.selectItemForUpdate(20L)).thenReturn(inboundItem("10", "0"));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1004", 1));
        when(docNoGenerator.next(DocNoGenerator.RECEIPT)).thenReturn("RC202610170001");

        assertThatThrownBy(() -> inboundService.receipt(1L, receiptRequest(12L, 20L, "1")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40001));
    }

    @Test
    @DisplayName("序列号已存在时返回 40906")
    void shouldRejectDuplicateSerialNo() {
        when(inboundOrderMapper.selectForUpdate(1L)).thenReturn(order(2));
        when(locationMapper.selectById(12L)).thenReturn(location(12L, 1L, 2));
        when(inboundOrderMapper.selectItemForUpdate(20L)).thenReturn(inboundItem("10", "0"));
        when(skuMapper.selectById(1L)).thenReturn(sku(1L, "SKU-1004", 1));
        when(docNoGenerator.next(DocNoGenerator.RECEIPT)).thenReturn("RC202610170001");
        when(serialMapper.selectBySerialNo("SN-001")).thenReturn(new com.wms.entity.Serial());

        ReceiptRequest request = receiptRequest(12L, 20L, "1");
        request.getItems().get(0).setSerialNo("SN-001");

        assertThatThrownBy(() -> inboundService.receipt(1L, request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40906));
    }
}
