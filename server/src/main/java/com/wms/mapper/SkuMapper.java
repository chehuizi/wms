package com.wms.mapper;

import com.wms.dto.SkuQuery;
import com.wms.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU Mapper，SQL 全部定义在 resources/mapper/SkuMapper.xml。
 */
@Mapper
public interface SkuMapper {

    List<Sku> selectPage(@Param("q") SkuQuery query);

    long countPage(@Param("q") SkuQuery query);

    Sku selectById(@Param("id") Long id);

    int insert(Sku sku);

    int update(Sku sku);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);

    /** 去重后的分类列表 */
    List<String> selectDistinctCategories();
}
