package com.wms.mapper;

import com.wms.dto.LocationQuery;
import com.wms.entity.Location;
import com.wms.vo.LocationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库位 Mapper，SQL 全部定义在 resources/mapper/LocationMapper.xml。
 */
@Mapper
public interface LocationMapper {

    List<LocationVO> selectPage(@Param("q") LocationQuery query);

    long countPage(@Param("q") LocationQuery query);

    LocationVO selectVOById(@Param("id") Long id);

    Location selectById(@Param("id") Long id);

    /** 批量取库位（波次释放时按库位编码排序用） */
    List<Location> selectByIds(@Param("ids") List<Long> ids);

    int insert(Location location);

    int update(Location location);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);

    /** 统计某仓库下未删除的库位数量，用于仓库删除前的级联保护校验 */
    long countActiveByWarehouseId(@Param("warehouseId") Long warehouseId);
}
