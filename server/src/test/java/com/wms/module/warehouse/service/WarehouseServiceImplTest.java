package com.wms.module.warehouse.service;

import com.wms.common.BizException;
import com.wms.entity.Warehouse;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.warehouse.service.WarehouseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓库业务规则单元测试。
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private LocationMapper locationMapper;
    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    @DisplayName("仓库下仍有未删除库位时拒绝删除，返回 40902")
    void shouldRejectDeleteWhenActiveLocationsExist() {
        when(warehouseMapper.selectById(1L)).thenReturn(existingWarehouse(1L));
        when(locationMapper.countActiveByWarehouseId(1L)).thenReturn(7L);

        assertThatThrownBy(() -> warehouseService.delete(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40902);
                    assertThat(biz.getHttpStatus()).isEqualTo(409);
                    assertThat(biz.getMessage()).contains("库位");
                });

        verify(warehouseMapper, never()).softDeleteById(anyLong());
    }

    @Test
    @DisplayName("仓库下无库位时允许软删除")
    void shouldDeleteWhenNoActiveLocations() {
        when(warehouseMapper.selectById(1L)).thenReturn(existingWarehouse(1L));
        when(locationMapper.countActiveByWarehouseId(1L)).thenReturn(0L);
        when(warehouseMapper.softDeleteById(1L)).thenReturn(1);

        warehouseService.delete(1L);

        verify(warehouseMapper).softDeleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的仓库返回 40401")
    void shouldThrowNotFoundWhenWarehouseMissing() {
        when(warehouseMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> warehouseService.delete(999L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40401));
    }

    @Test
    @DisplayName("编码重复时 DuplicateKeyException 被转换为 40901，而不是 500")
    void shouldTranslateDuplicateKeyToBusinessError() {
        com.wms.dto.WarehouseSaveRequest request = new com.wms.dto.WarehouseSaveRequest();
        request.setCode("WH01");
        request.setName("重复仓库");

        when(warehouseMapper.insert(any(Warehouse.class)))
                .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> warehouseService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40901);
                    assertThat(biz.getHttpStatus()).isEqualTo(409);
                    assertThat(biz.getMessage()).contains("已存在");
                });
    }

    @Test
    @DisplayName("新建时未传 status 默认落 1（启用）")
    void shouldDefaultStatusToEnabledOnCreate() {
        com.wms.dto.WarehouseSaveRequest request = new com.wms.dto.WarehouseSaveRequest();
        request.setCode("WH99");
        request.setName("新仓库");

        when(warehouseMapper.insert(any(Warehouse.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Warehouse.class).setId(10L);
            return 1;
        });
        when(warehouseMapper.selectById(10L)).thenReturn(existingWarehouse(10L));

        warehouseService.create(request);

        org.mockito.ArgumentCaptor<Warehouse> captor =
                org.mockito.ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    private Warehouse existingWarehouse(Long id) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setCode("WH01");
        w.setName("华东中心仓");
        w.setStatus(1);
        w.setDeleted(0);
        return w;
    }
}
