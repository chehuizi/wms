package com.wms.module.location.service;

import com.wms.common.BizException;
import com.wms.dto.LocationSaveRequest;
import com.wms.entity.Zone;
import com.wms.entity.Warehouse;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.ZoneMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.module.location.service.LocationServiceImpl;
import com.wms.vo.LocationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库位业务规则单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationMapper locationMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private ZoneMapper zoneMapper;
    @InjectMocks
    private LocationServiceImpl locationService;

    @Test
    @DisplayName("库位引用不存在的仓库时返回 40003")
    void shouldRejectNonExistentWarehouse() {
        LocationSaveRequest request = baseRequest();
        when(warehouseMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40003);
                    assertThat(biz.getHttpStatus()).isEqualTo(400);
                });

        verify(locationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("库位引用已停用的仓库时返回 40003")
    void shouldRejectDisabledWarehouse() {
        LocationSaveRequest request = baseRequest();

        Warehouse disabled = new Warehouse();
        disabled.setId(1L);
        disabled.setCode("WH03");
        disabled.setName("华南前置仓");
        disabled.setStatus(0);
        when(warehouseMapper.selectById(1L)).thenReturn(disabled);

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40003));

        verify(locationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("仓库启用时创建成功，未传的枚举字段落默认值")
    void shouldCreateWithDefaultsWhenWarehouseActive() {
        LocationSaveRequest request = baseRequest();

        Warehouse active = new Warehouse();
        active.setId(1L);
        active.setName("华东中心仓");
        active.setStatus(1);
        when(warehouseMapper.selectById(1L)).thenReturn(active);

        Zone zone = new Zone();
        zone.setId(1L);
        zone.setWarehouseId(1L);
        zone.setCode("A");
        zone.setName("A区");
        when(zoneMapper.selectByCodeAndWarehouseId("A", 1L)).thenReturn(zone);

        // 模拟数据库回填主键，随后 create 会用该 id 回查详情
        when(locationMapper.insert(any(com.wms.entity.Location.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, com.wms.entity.Location.class).setId(1L);
            return 1;
        });
        when(locationMapper.selectVOById(1L)).thenReturn(sampleVO());

        locationService.create(request);

        org.mockito.ArgumentCaptor<com.wms.entity.Location> captor =
                org.mockito.ArgumentCaptor.forClass(com.wms.entity.Location.class);
        verify(locationMapper).insert(captor.capture());
        assertThat(captor.getValue().getLocationType()).isEqualTo(1);
        assertThat(captor.getValue().getAllowNegative()).isEqualTo(0);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("查询详情时返回所属仓库名称 warehouseName")
    void shouldReturnWarehouseNameInDetail() {
        LocationVO vo = sampleVO();
        when(locationMapper.selectVOById(1L)).thenReturn(vo);

        LocationVO result = locationService.getById(1L);

        assertThat(result.getWarehouseName()).isEqualTo("华东中心仓");
        assertThat(result.getCode()).isEqualTo("A-01-01");
    }

    private LocationSaveRequest baseRequest() {
        LocationSaveRequest request = new LocationSaveRequest();
        request.setWarehouseId(1L);
        request.setCode("A-09-01");
        request.setZone("A");
        return request;
    }

    private LocationVO sampleVO() {
        LocationVO vo = new LocationVO();
        vo.setId(1L);
        vo.setWarehouseId(1L);
        vo.setWarehouseName("华东中心仓");
        vo.setCode("A-01-01");
        vo.setZone("A");
        vo.setLocationType(1);
        vo.setStatus(1);
        return vo;
    }
}
