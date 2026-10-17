package com.wms.module.serial.controller;

import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.dto.SerialQuery;
import com.wms.module.stock.SerialService;
import com.wms.vo.SerialVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 序列号台账，见契约第 14 节。
 */
@RestController
@RequestMapping("/api/v1/serials")
public class SerialController {

    private final SerialService serialService;

    public SerialController(SerialService serialService) {
        this.serialService = serialService;
    }

    @GetMapping
    public Result<PageResult<SerialVO>> page(SerialQuery query) {
        return Result.ok(serialService.page(query));
    }

    @GetMapping("/{id}")
    public Result<SerialVO> getById(@PathVariable Long id) {
        return Result.ok(serialService.getById(id));
    }
}
