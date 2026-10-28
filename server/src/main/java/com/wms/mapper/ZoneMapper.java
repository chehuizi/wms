package com.wms.mapper;

import com.wms.dto.ZoneQuery;
import com.wms.entity.Zone;
import com.wms.vo.ZoneVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库区 Mapper，SQL 全部定义在 resources/mapper/ZoneMapper.xml。
 */
@Mapper
public interface ZoneMapper {

    List<ZoneVO> selectPage(@Param("q") ZoneQuery query);

    long countPage(@Param("q") ZoneQuery query);

    ZoneVO selectVOById(@Param("id") Long id);

    Zone selectById(@Param("id") Long id);

    Zone selectByCode(@Param("warehouseId") Long warehouseId, @Param("code") String code);

    int insert(Zone zone);

    int update(Zone zone);

    int softDeleteById(@Param("id") Long id);

    /** 统计某库区下未删除的库位数量，用于库区删除前的级联保护校验 */
    long countActiveLocations(@Param("warehouseId") Long warehouseId, @Param("zone") String zone);
}
