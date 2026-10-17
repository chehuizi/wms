package com.wms.module.inventory.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.InventoryAdjustRequest;
import com.wms.dto.InventoryQuery;
import com.wms.dto.MovementQuery;
import com.wms.module.stock.InventoryService;
import com.wms.vo.InventoryVO;
import com.wms.vo.StockMovementVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存台账与库存流水，见契约第 13 节。
 */
@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Result<PageResult<InventoryVO>> page(InventoryQuery query) {
        return Result.ok(inventoryService.page(query));
    }

    /** 精确路径必须声明在 /{id} 之前，避免 id 被当作其他参数 */
    @GetMapping("/movements")
    public Result<PageResult<StockMovementVO>> movements(MovementQuery query) {
        return Result.ok(inventoryService.pageMovements(query));
    }

    @GetMapping("/{id}")
    public Result<InventoryVO> getById(@PathVariable Long id) {
        return Result.ok(inventoryService.getById(id));
    }

    /** 手工调整，流水 movementType = 9 */
    @PostMapping("/adjust")
    public Result<InventoryVO> adjust(@Valid @RequestBody InventoryAdjustRequest request) {
        return Result.ok(inventoryService.adjust(request));
    }
}
