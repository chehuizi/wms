package com.wms.module.customer.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.CustomerQuery;
import com.wms.dto.CustomerSaveRequest;
import com.wms.entity.Customer;
import com.wms.mapper.CustomerMapper;
import com.wms.module.customer.CustomerService;
import com.wms.vo.CustomerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class CustomerServiceImpl implements CustomerService {

    /**
     * 邮箱格式校验。@Email 已覆盖常规场景，这里再兜一层：
     * 1) 空串归一化为 null，避免 @Email 对空串报错；
     * 2) 给出契约要求的 40002。
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResult<CustomerVO> page(CustomerQuery query) {
        long total = customerMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<CustomerVO> list = customerMapper.selectPage(query).stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public CustomerVO getById(Long id) {
        return toVO(requireExists(id));
    }

    @Override
    public CustomerVO create(CustomerSaveRequest request) {
        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);
        customer.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        customer.setContactEmail(normalizeEmail(request.getContactEmail()));
        try {
            customerMapper.insert(customer);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("客户编码已存在: " + request.getCode());
        }
        return getById(customer.getId());
    }

    @Override
    @Transactional
    public CustomerVO update(Long id, CustomerSaveRequest request) {
        requireExists(id);
        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);
        customer.setId(id);
        customer.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        customer.setContactEmail(normalizeEmail(request.getContactEmail()));
        try {
            customerMapper.update(customer);
        } catch (DuplicateKeyException e) {
            throw BizException.duplicateCode("客户编码已存在: " + request.getCode());
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        customerMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            requireExists(id);
        }
        customerMapper.softDeleteByIds(ids);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw BizException.emailInvalid("contactEmail: 邮箱格式不正确");
        }
        return trimmed;
    }

    private Customer requireExists(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BizException.notFound("客户不存在: " + id);
        }
        return customer;
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }
}
