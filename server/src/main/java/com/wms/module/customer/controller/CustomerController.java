package com.wms.module.customer.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.CustomerQuery;
import com.wms.dto.CustomerSaveRequest;
import com.wms.module.customer.CustomerService;
import com.wms.vo.CustomerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Result<PageResult<CustomerVO>> page(CustomerQuery query) {
        return Result.ok(customerService.page(query));
    }

    @GetMapping("/{id}")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        return Result.ok(customerService.getById(id));
    }

    @PostMapping
    public Result<CustomerVO> create(@Valid @RequestBody CustomerSaveRequest request) {
        return Result.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    public Result<CustomerVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CustomerSaveRequest request) {
        return Result.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        customerService.batchDelete(ids);
        return Result.ok();
    }
}
