package com.wms.module.outbound;

import com.wms.common.PageResult;
import com.wms.dto.OutboundOrderQuery;
import com.wms.dto.OutboundOrderSaveRequest;
import com.wms.dto.ShipRequest;
import com.wms.vo.AllocationVO;
import com.wms.vo.OutboundOrderDetailVO;
import com.wms.vo.OutboundOrderVO;
import com.wms.vo.PickTaskVO;
import com.wms.vo.ShipmentVO;

import java.util.List;

/**
 * 出库：订单 → 分配锁定 → 拣货 → 发货，见契约第 16 节。
 */
public interface OutboundService {

    PageResult<OutboundOrderVO> page(OutboundOrderQuery query);

    OutboundOrderDetailVO getById(Long id);

    OutboundOrderDetailVO create(OutboundOrderSaveRequest request);

    OutboundOrderDetailVO update(Long id, OutboundOrderSaveRequest request);

    void delete(Long id);

    OutboundOrderDetailVO submit(Long id);

    /** FIFO 分配库存并锁定；可用量不足返回 40903 且不留任何锁定 */
    OutboundOrderDetailVO allocate(Long id);

    /** 直接生成拣货任务（不走波次） */
    OutboundOrderDetailVO createPickTasks(Long id);

    /** 释放全部未发货锁定，订单回到待分配 */
    OutboundOrderDetailVO releaseAllocation(Long id);

    OutboundOrderDetailVO cancel(Long id);

    ShipmentVO ship(Long id, ShipRequest request);

    List<ShipmentVO> shipments(Long id);

    List<AllocationVO> allocations(Long id);

    List<PickTaskVO> pickTasks(Long id);
}
