package com.wms.module.outbound.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.OutboundOrderQuery;
import com.wms.dto.OutboundOrderSaveRequest;
import com.wms.dto.ShipRequest;
import com.wms.module.outbound.OutboundService;
import com.wms.vo.AllocationVO;
import com.wms.vo.OutboundOrderDetailVO;
import com.wms.vo.OutboundOrderVO;
import com.wms.vo.PickTaskVO;
import com.wms.vo.ShipmentVO;
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

/**
 * 出库订单，见契约第 16 节。
 */
@RestController
@RequestMapping("/api/v1/outbound-orders")
public class OutboundOrderController {

    private final OutboundService outboundService;

    public OutboundOrderController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping
    public Result<PageResult<OutboundOrderVO>> page(OutboundOrderQuery query) {
        return Result.ok(outboundService.page(query));
    }

    @GetMapping("/{id}")
    public Result<OutboundOrderDetailVO> getById(@PathVariable Long id) {
        return Result.ok(outboundService.getById(id));
    }

    @PostMapping
    public Result<OutboundOrderDetailVO> create(@Valid @RequestBody OutboundOrderSaveRequest request) {
        return Result.ok(outboundService.create(request));
    }

    @PutMapping("/{id}")
    public Result<OutboundOrderDetailVO> update(@PathVariable Long id,
                                                @Valid @RequestBody OutboundOrderSaveRequest request) {
        return Result.ok(outboundService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        outboundService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    public Result<OutboundOrderDetailVO> submit(@PathVariable Long id) {
        return Result.ok(outboundService.submit(id));
    }

    /** 分配库存（锁定）；可用量不足返回 40903 且不留任何锁定 */
    @PostMapping("/{id}/allocate")
    public Result<OutboundOrderDetailVO> allocate(@PathVariable Long id) {
        return Result.ok(outboundService.allocate(id));
    }

    /** 直接生成拣货任务（不走波次） */
    @PostMapping("/{id}/pick-tasks")
    public Result<OutboundOrderDetailVO> pickTasks(@PathVariable Long id) {
        return Result.ok(outboundService.createPickTasks(id));
    }

    @PostMapping("/{id}/release-allocation")
    public Result<OutboundOrderDetailVO> releaseAllocation(@PathVariable Long id) {
        return Result.ok(outboundService.releaseAllocation(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<OutboundOrderDetailVO> cancel(@PathVariable Long id) {
        return Result.ok(outboundService.cancel(id));
    }

    /** 发货：扣减在库量与锁定量，写 2 号流水 */
    @PostMapping("/{id}/ship")
    public Result<ShipmentVO> ship(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        return Result.ok(outboundService.ship(id, request));
    }

    @GetMapping("/{id}/shipments")
    public Result<List<ShipmentVO>> shipments(@PathVariable Long id) {
        return Result.ok(outboundService.shipments(id));
    }

    @GetMapping("/{id}/allocations")
    public Result<List<AllocationVO>> allocations(@PathVariable Long id) {
        return Result.ok(outboundService.allocations(id));
    }

    @GetMapping("/{id}/pick-tasks")
    public Result<List<PickTaskVO>> listPickTasks(@PathVariable Long id) {
        return Result.ok(outboundService.pickTasks(id));
    }
}
