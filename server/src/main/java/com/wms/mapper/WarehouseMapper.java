package com.wms.mapper;

import com.wms.dto.WarehouseQuery;
import com.wms.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仓库 Mapper，SQL 全部定义在 resources/mapper/WarehouseMapper.xml。
 */
@Mapper
public interface WarehouseMapper {

    List<Warehouse> selectPage(@Param("q") WarehouseQuery query);

    long countPage(@Param("q") WarehouseQuery query);

    Warehouse selectById(@Param("id") Long id);

    Warehouse selectByCode(@Param("code") String code);

    int insert(Warehouse warehouse);

    int update(Warehouse warehouse);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);
}
