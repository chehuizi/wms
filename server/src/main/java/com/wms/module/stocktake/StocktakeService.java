package com.wms.module.stocktake;

import com.wms.common.PageResult;
import com.wms.dto.StocktakeApproveRequest;
import com.wms.dto.StocktakeCountRequest;
import com.wms.dto.StocktakeCreateRequest;
import com.wms.dto.StocktakeOrderQuery;
import com.wms.vo.StocktakeOrderDetailVO;
import com.wms.vo.StocktakeOrderVO;

/**
 * 盘点：盘点单 → 实盘 → 差异审批 → 调账，见契约第 17 节。
 */
public interface StocktakeService {

    PageResult<StocktakeOrderVO> page(StocktakeOrderQuery query);

    StocktakeOrderDetailVO getById(Long id);

    /** 创建并按范围内现有库存生成明细 */
    StocktakeOrderDetailVO create(StocktakeCreateRequest request);

    void delete(Long id);

    StocktakeOrderDetailVO start(Long id);

    StocktakeOrderDetailVO count(Long id, StocktakeCountRequest request);

    StocktakeOrderDetailVO finish(Long id);

    StocktakeOrderDetailVO approve(Long id, StocktakeApproveRequest request);

    StocktakeOrderDetailVO cancel(Long id);
}
