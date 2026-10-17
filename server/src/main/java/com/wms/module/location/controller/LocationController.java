package com.wms.module.location.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.LocationQuery;
import com.wms.dto.LocationSaveRequest;
import com.wms.module.location.LocationService;
import com.wms.vo.LocationVO;
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
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public Result<PageResult<LocationVO>> page(LocationQuery query) {
        return Result.ok(locationService.page(query));
    }

    @GetMapping("/{id}")
    public Result<LocationVO> getById(@PathVariable Long id) {
        return Result.ok(locationService.getById(id));
    }

    @PostMapping
    public Result<LocationVO> create(@Valid @RequestBody LocationSaveRequest request) {
        return Result.ok(locationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<LocationVO> update(@PathVariable Long id,
                                     @Valid @RequestBody LocationSaveRequest request) {
        return Result.ok(locationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        locationService.batchDelete(ids);
        return Result.ok();
    }
}
