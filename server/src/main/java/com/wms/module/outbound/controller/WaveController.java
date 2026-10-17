package com.wms.module.outbound.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.WaveCreateRequest;
import com.wms.dto.WaveOrdersRequest;
import com.wms.dto.WaveQuery;
import com.wms.module.outbound.WaveService;
import com.wms.vo.WaveVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拣货波次，见契约第 16.3 节。
 */
@RestController
@RequestMapping("/api/v1/waves")
public class WaveController {

    private final WaveService waveService;

    public WaveController(WaveService waveService) {
        this.waveService = waveService;
    }

    @GetMapping
    public Result<PageResult<WaveVO>> page(WaveQuery query) {
        return Result.ok(waveService.page(query));
    }

    @GetMapping("/{id}")
    public Result<WaveVO> getById(@PathVariable Long id) {
        return Result.ok(waveService.getById(id));
    }

    @PostMapping
    public Result<WaveVO> create(@Valid @RequestBody WaveCreateRequest request) {
        return Result.ok(waveService.create(request));
    }

    @PostMapping("/{id}/orders")
    public Result<WaveVO> addOrders(@PathVariable Long id, @Valid @RequestBody WaveOrdersRequest request) {
        return Result.ok(waveService.addOrders(id, request));
    }

    /** 释放波次：按库位编码升序生成拣货任务 */
    @PostMapping("/{id}/release")
    public Result<WaveVO> release(@PathVariable Long id) {
        return Result.ok(waveService.release(id));
    }
}
