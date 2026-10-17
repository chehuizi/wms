package com.wms.module.customer;

import com.wms.common.PageResult;
import com.wms.dto.CustomerQuery;
import com.wms.dto.CustomerSaveRequest;
import com.wms.vo.CustomerVO;

import java.util.List;

public interface CustomerService {

    PageResult<CustomerVO> page(CustomerQuery query);

    CustomerVO getById(Long id);

    CustomerVO create(CustomerSaveRequest request);

    CustomerVO update(Long id, CustomerSaveRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);
}
