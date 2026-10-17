package com.wms.mapper;

import com.wms.dto.MovementQuery;
import com.wms.entity.StockMovement;
import com.wms.vo.StockMovementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存流水 Mapper，SQL 全部定义在 resources/mapper/StockMovementMapper.xml。
 * 流水只增不改不删，本 Mapper 只有查询与插入。
 */
@Mapper
public interface StockMovementMapper {

    List<StockMovementVO> selectPage(@Param("q") MovementQuery query);

    long countPage(@Param("q") MovementQuery query);

    int insert(StockMovement movement);
}
