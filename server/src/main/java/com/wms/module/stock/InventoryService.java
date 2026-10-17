package com.wms.module.stock;

import com.wms.common.PageResult;
import com.wms.dto.InventoryAdjustRequest;
import com.wms.dto.InventoryQuery;
import com.wms.dto.MovementQuery;
import com.wms.vo.InventoryVO;
import com.wms.vo.StockMovementVO;

/**
 * 库存台账查询与手工调整，见契约第 13 节。
 */
public interface InventoryService {

    PageResult<InventoryVO> page(InventoryQuery query);

    InventoryVO getById(Long id);

    /** 手工调整，流水 movementType = 9 */
    InventoryVO adjust(InventoryAdjustRequest request);

    PageResult<StockMovementVO> pageMovements(MovementQuery query);
}
