package com.wms.module.warehouse.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.WarehouseQuery;
import com.wms.dto.WarehouseSaveRequest;
import com.wms.module.warehouse.WarehouseService;
import com.wms.vo.WarehouseVO;
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
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public Result<PageResult<WarehouseVO>> page(WarehouseQuery query) {
        return Result.ok(warehouseService.page(query));
    }

    @GetMapping("/{id}")
    public Result<WarehouseVO> getById(@PathVariable Long id) {
        return Result.ok(warehouseService.getById(id));
    }

    @PostMapping
    public Result<WarehouseVO> create(@Valid @RequestBody WarehouseSaveRequest request) {
        return Result.ok(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    public Result<WarehouseVO> update(@PathVariable Long id,
                                      @Valid @RequestBody WarehouseSaveRequest request) {
        return Result.ok(warehouseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        warehouseService.batchDelete(ids);
        return Result.ok();
    }
}
