package com.wms.mapper;

import com.wms.entity.Zone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库区 Mapper，SQL 全部定义在 resources/mapper/ZoneMapper.xml。
 */
@Mapper
public interface ZoneMapper {

    List<Zone> selectPage(@Param("q") com.wms.dto.ZoneQuery query);

    long countPage(@Param("q") com.wms.dto.ZoneQuery query);

    Zone selectById(@Param("id") Long id);

    Zone selectByCodeAndWarehouseId(@Param("code") String code,
                                     @Param("warehouseId") Long warehouseId);

    int insert(Zone zone);

    int update(Zone zone);

    int softDeleteById(@Param("id") Long id);

    long countActiveByWarehouseId(@Param("warehouseId") Long warehouseId);

    long countActiveByZoneCodeAndWarehouseId(@Param("code") String code,
                                              @Param("warehouseId") Long warehouseId);
}
