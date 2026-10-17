package com.wms.module.inbound.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.PutawayExecuteRequest;
import com.wms.dto.PutawayTaskQuery;
import com.wms.module.inbound.PutawayService;
import com.wms.vo.PutawayTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 上架任务，见契约第 15.2 节。
 */
@RestController
@RequestMapping("/api/v1/putaway-tasks")
public class PutawayTaskController {

    private final PutawayService putawayService;

    public PutawayTaskController(PutawayService putawayService) {
        this.putawayService = putawayService;
    }

    @GetMapping
    public Result<PageResult<PutawayTaskVO>> page(PutawayTaskQuery query) {
        return Result.ok(putawayService.page(query));
    }

    /** 执行上架：源位减、目标位加，产生 5/6 两条流水 */
    @PostMapping("/{id}/execute")
    public Result<PutawayTaskVO> execute(@PathVariable Long id,
                                         @Valid @RequestBody PutawayExecuteRequest request) {
        return Result.ok(putawayService.execute(id, request));
    }
}
