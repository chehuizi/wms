package com.wms.module.inbound.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.InboundOrderQuery;
import com.wms.dto.InboundOrderSaveRequest;
import com.wms.dto.ReceiptRequest;
import com.wms.module.inbound.InboundService;
import com.wms.vo.InboundOrderDetailVO;
import com.wms.vo.InboundOrderVO;
import com.wms.vo.InboundReceiptVO;
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
 * 入库到货单，见契约第 15 节。
 */
@RestController
@RequestMapping("/api/v1/inbound-orders")
public class InboundOrderController {

    private final InboundService inboundService;

    public InboundOrderController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @GetMapping
    public Result<PageResult<InboundOrderVO>> page(InboundOrderQuery query) {
        return Result.ok(inboundService.page(query));
    }

    @GetMapping("/{id}")
    public Result<InboundOrderDetailVO> getById(@PathVariable Long id) {
        return Result.ok(inboundService.getById(id));
    }

    @PostMapping
    public Result<InboundOrderDetailVO> create(@Valid @RequestBody InboundOrderSaveRequest request) {
        return Result.ok(inboundService.create(request));
    }

    @PutMapping("/{id}")
    public Result<InboundOrderDetailVO> update(@PathVariable Long id,
                                               @Valid @RequestBody InboundOrderSaveRequest request) {
        return Result.ok(inboundService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inboundService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    public Result<InboundOrderDetailVO> submit(@PathVariable Long id) {
        return Result.ok(inboundService.submit(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<InboundOrderDetailVO> cancel(@PathVariable Long id) {
        return Result.ok(inboundService.cancel(id));
    }

    /** 执行收货 */
    @PostMapping("/{id}/receipts")
    public Result<InboundReceiptVO> receipt(@PathVariable Long id,
                                            @Valid @RequestBody ReceiptRequest request) {
        return Result.ok(inboundService.receipt(id, request));
    }

    @GetMapping("/{id}/receipts")
    public Result<List<InboundReceiptVO>> receipts(@PathVariable Long id) {
        return Result.ok(inboundService.listReceipts(id));
    }
}
