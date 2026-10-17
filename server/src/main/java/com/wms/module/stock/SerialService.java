package com.wms.module.stock;

import com.wms.common.PageResult;
import com.wms.dto.SerialQuery;
import com.wms.vo.SerialVO;

/**
 * 序列号台账，见契约第 14 节。
 */
public interface SerialService {

    PageResult<SerialVO> page(SerialQuery query);

    SerialVO getById(Long id);
}
