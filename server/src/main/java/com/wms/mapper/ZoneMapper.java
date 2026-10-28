package com.wms.mapper;

import com.wms.dto.ZoneQuery;
import com.wms.entity.Zone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库区 Mapper，SQL 全部定义在 resources/mapper/ZoneMapper.xml。
 */
@Mapper
public interface ZoneMapper {

    List<Zone> selectPage(@Param("q") ZoneQuery query);

    long countPage(@Param("q") ZoneQuery query);

    Zone selectById(@Param("id") Long id);

    Zone selectByWarehouseIdAndCode(@Param("warehouseId") Long warehouseId,
                                      @Param("code") String code);

    int insert(Zone zone);

    int update(Zone zone);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);

    long countActiveByWarehouseId(@Param("warehouseId") Long warehouseId);

    long countActiveLocationsByZoneId(@Param("zoneId") Long zoneId);
}
