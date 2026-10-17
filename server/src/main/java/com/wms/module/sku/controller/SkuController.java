package com.wms.module.sku.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.SkuQuery;
import com.wms.dto.SkuSaveRequest;
import com.wms.module.sku.SkuService;
import com.wms.vo.SkuVO;
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
@RequestMapping("/api/v1/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping
    public Result<PageResult<SkuVO>> page(SkuQuery query) {
        return Result.ok(skuService.page(query));
    }

    /** 注意：本方法必须声明在 /{id} 之前或使用精确路径，避免 id 被当作数字解析失败 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(skuService.categories());
    }

    @GetMapping("/{id}")
    public Result<SkuVO> getById(@PathVariable Long id) {
        return Result.ok(skuService.getById(id));
    }

    @PostMapping
    public Result<SkuVO> create(@Valid @RequestBody SkuSaveRequest request) {
        return Result.ok(skuService.create(request));
    }

    @PutMapping("/{id}")
    public Result<SkuVO> update(@PathVariable Long id, @Valid @RequestBody SkuSaveRequest request) {
        return Result.ok(skuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        skuService.batchDelete(ids);
        return Result.ok();
    }
}
