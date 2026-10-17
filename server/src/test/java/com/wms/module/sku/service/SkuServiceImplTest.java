package com.wms.module.sku.service;

import com.wms.dto.SkuSaveRequest;
import com.wms.entity.Sku;
import com.wms.mapper.SkuMapper;
import com.wms.module.sku.service.SkuServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SKU 业务规则单元测试，重点验证 shelfLifeDays = 0 落库为 NULL 的约定。
 */
@ExtendWith(MockitoExtension.class)
class SkuServiceImplTest {

    @Mock
    private SkuMapper skuMapper;
    @InjectMocks
    private SkuServiceImpl skuService;

    @Test
    @DisplayName("shelfLifeDays 为 0 时按无保质期处理，落库为 NULL")
    void shouldStoreNullShelfLifeWhenZero() {
        SkuSaveRequest request = new SkuSaveRequest();
        request.setCode("SKU-9001");
        request.setName("测试商品");
        request.setShelfLifeDays(0);

        when(skuMapper.insert(any(Sku.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Sku.class).setId(99L);
            return 1;
        });
        when(skuMapper.selectById(anyLong())).thenReturn(new Sku());

        skuService.create(request);

        ArgumentCaptor<Sku> captor = ArgumentCaptor.forClass(Sku.class);
        verify(skuMapper).insert(captor.capture());
        assertThat(captor.getValue().getShelfLifeDays()).isNull();
        assertThat(captor.getValue().getBatchManaged()).isZero();
        assertThat(captor.getValue().getSerialManaged()).isZero();
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("shelfLifeDays 为正数时原样保留")
    void shouldKeepPositiveShelfLife() {
        SkuSaveRequest request = new SkuSaveRequest();
        request.setCode("SKU-9002");
        request.setName("测试商品2");
        request.setShelfLifeDays(365);

        when(skuMapper.insert(any(Sku.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Sku.class).setId(100L);
            return 1;
        });
        when(skuMapper.selectById(anyLong())).thenReturn(new Sku());

        skuService.create(request);

        ArgumentCaptor<Sku> captor = ArgumentCaptor.forClass(Sku.class);
        verify(skuMapper).insert(captor.capture());
        assertThat(captor.getValue().getShelfLifeDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("分类列表直接透传去重后的结果")
    void shouldReturnDistinctCategories() {
        when(skuMapper.selectDistinctCategories()).thenReturn(java.util.List.of("饮料", "方便速食"));

        assertThat(skuService.categories()).containsExactly("饮料", "方便速食");
    }
}
