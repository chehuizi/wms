package com.wms.module.stocktake.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.StocktakeApproveRequest;
import com.wms.dto.StocktakeCountRequest;
import com.wms.dto.StocktakeCreateRequest;
import com.wms.dto.StocktakeOrderQuery;
import com.wms.module.stocktake.StocktakeService;
import com.wms.vo.StocktakeOrderDetailVO;
import com.wms.vo.StocktakeOrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 盘点单，见契约第 17 节。
 */
@RestController
@RequestMapping("/api/v1/stocktake-orders")
public class StocktakeOrderController {

    private final StocktakeService stocktakeService;

    public StocktakeOrderController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @GetMapping
    public Result<PageResult<StocktakeOrderVO>> page(StocktakeOrderQuery query) {
        return Result.ok(stocktakeService.page(query));
    }

    @GetMapping("/{id}")
    public Result<StocktakeOrderDetailVO> getById(@PathVariable Long id) {
        return Result.ok(stocktakeService.getById(id));
    }

    @PostMapping
    public Result<StocktakeOrderDetailVO> create(@Valid @RequestBody StocktakeCreateRequest request) {
        return Result.ok(stocktakeService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stocktakeService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/start")
    public Result<StocktakeOrderDetailVO> start(@PathVariable Long id) {
        return Result.ok(stocktakeService.start(id));
    }

    /** 录入实盘，没货要显式录 0 */
    @PostMapping("/{id}/count")
    public Result<StocktakeOrderDetailVO> count(@PathVariable Long id,
                                                @Valid @RequestBody StocktakeCountRequest request) {
        return Result.ok(stocktakeService.count(id, request));
    }

    @PostMapping("/{id}/finish")
    public Result<StocktakeOrderDetailVO> finish(@PathVariable Long id) {
        return Result.ok(stocktakeService.finish(id));
    }

    /** 审批并调账 */
    @PostMapping("/{id}/approve")
    public Result<StocktakeOrderDetailVO> approve(@PathVariable Long id,
                                                  @Valid @RequestBody StocktakeApproveRequest request) {
        return Result.ok(stocktakeService.approve(id, request));
    }

    @PostMapping("/{id}/cancel")
    public Result<StocktakeOrderDetailVO> cancel(@PathVariable Long id) {
        return Result.ok(stocktakeService.cancel(id));
    }
}
