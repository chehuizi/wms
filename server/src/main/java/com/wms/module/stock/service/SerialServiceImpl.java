package com.wms.module.stock.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.SerialQuery;
import com.wms.mapper.SerialMapper;
import com.wms.module.stock.SerialService;
import com.wms.vo.SerialVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 序列号台账查询实现，见契约第 14 节。本模块只读，写操作由出入库流程触发。
 */
@Service
public class SerialServiceImpl implements SerialService {

    private final SerialMapper serialMapper;

    public SerialServiceImpl(SerialMapper serialMapper) {
        this.serialMapper = serialMapper;
    }

    @Override
    public PageResult<SerialVO> page(SerialQuery query) {
        long total = serialMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        List<SerialVO> list = serialMapper.selectPage(query);
        return new PageResult<>(list, total, query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public SerialVO getById(Long id) {
        SerialVO vo = serialMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("序列号不存在: " + id);
        }
        return vo;
    }
}
