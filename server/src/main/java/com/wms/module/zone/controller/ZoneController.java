package com.wms.module.zone.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.ZoneQuery;
import com.wms.dto.ZoneSaveRequest;
import com.wms.module.zone.ZoneService;
import com.wms.vo.ZoneVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    public Result<PageResult<ZoneVO>> page(ZoneQuery query) {
        return Result.ok(zoneService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ZoneVO> getById(@PathVariable Long id) {
        return Result.ok(zoneService.getById(id));
    }

    @PostMapping
    public Result<ZoneVO> create(@Valid @RequestBody ZoneSaveRequest request) {
        return Result.ok(zoneService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ZoneVO> update(@PathVariable Long id,
                                 @Valid @RequestBody ZoneSaveRequest request) {
        return Result.ok(zoneService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return Result.ok();
    }
}
