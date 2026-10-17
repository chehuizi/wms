package com.wms.module.outbound.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.PickRequest;
import com.wms.dto.PickTaskQuery;
import com.wms.module.outbound.PickTaskService;
import com.wms.vo.PickTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拣货任务，见契约第 16.4 节。
 */
@RestController
@RequestMapping("/api/v1/pick-tasks")
public class PickTaskController {

    private final PickTaskService pickTaskService;

    public PickTaskController(PickTaskService pickTaskService) {
        this.pickTaskService = pickTaskService;
    }

    @GetMapping
    public Result<PageResult<PickTaskVO>> page(PickTaskQuery query) {
        return Result.ok(pickTaskService.page(query));
    }

    @PostMapping("/{id}/pick")
    public Result<PickTaskVO> pick(@PathVariable Long id, @Valid @RequestBody PickRequest request) {
        return Result.ok(pickTaskService.pick(id, request));
    }

    @PostMapping("/{id}/cancel")
    public Result<PickTaskVO> cancel(@PathVariable Long id) {
        return Result.ok(pickTaskService.cancel(id));
    }
}
