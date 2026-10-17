package com.wms.module.inbound;

import com.wms.common.PageResult;
import com.wms.dto.InboundOrderQuery;
import com.wms.dto.InboundOrderSaveRequest;
import com.wms.dto.PutawayTaskQuery;
import com.wms.dto.ReceiptRequest;
import com.wms.vo.InboundOrderDetailVO;
import com.wms.vo.InboundOrderVO;
import com.wms.vo.InboundReceiptVO;
import com.wms.vo.PutawayTaskVO;

import java.util.List;

/**
 * 入库：到货单 → 收货 → 上架，见契约第 15 节。
 */
public interface InboundService {

    PageResult<InboundOrderVO> page(InboundOrderQuery query);

    InboundOrderDetailVO getById(Long id);

    InboundOrderDetailVO create(InboundOrderSaveRequest request);

    InboundOrderDetailVO update(Long id, InboundOrderSaveRequest request);

    void delete(Long id);

    InboundOrderDetailVO submit(Long id);

    InboundOrderDetailVO cancel(Long id);

    /** 执行收货，落库存并生成上架任务 */
    InboundReceiptVO receipt(Long id, ReceiptRequest request);

    List<InboundReceiptVO> listReceipts(Long id);

    PageResult<PutawayTaskVO> pagePutawayTasks(PutawayTaskQuery query);
}
