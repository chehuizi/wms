package com.wms.module.customer.service;

import com.wms.common.BizException;
import com.wms.dto.CustomerSaveRequest;
import com.wms.entity.Customer;
import com.wms.mapper.CustomerMapper;
import com.wms.module.customer.service.CustomerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户业务规则单元测试，重点验证邮箱处理。
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;
    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("邮箱格式非法时返回 40002")
    void shouldRejectInvalidEmail() {
        CustomerSaveRequest request = baseRequest();
        request.setContactEmail("not-an-email");

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(40002);
                    assertThat(biz.getHttpStatus()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("邮箱为空串时归一化为 null，不触发格式错误")
    void shouldNormalizeBlankEmailToNull() {
        CustomerSaveRequest request = baseRequest();
        request.setContactEmail("   ");

        when(customerMapper.insert(any(Customer.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Customer.class).setId(1L);
            return 1;
        });
        when(customerMapper.selectById(anyLong())).thenReturn(new Customer());

        customerService.create(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getContactEmail()).isNull();
    }

    @Test
    @DisplayName("合法邮箱去除首尾空格后落库")
    void shouldTrimValidEmail() {
        CustomerSaveRequest request = baseRequest();
        request.setContactEmail("  chenming@example.com  ");

        when(customerMapper.insert(any(Customer.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Customer.class).setId(2L);
            return 1;
        });
        when(customerMapper.selectById(anyLong())).thenReturn(new Customer());

        customerService.create(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getContactEmail()).isEqualTo("chenming@example.com");
    }

    private CustomerSaveRequest baseRequest() {
        CustomerSaveRequest request = new CustomerSaveRequest();
        request.setCode("CUS900");
        request.setName("测试客户");
        return request;
    }
}
