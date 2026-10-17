package com.wms.mapper;

import com.wms.dto.CustomerQuery;
import com.wms.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户 Mapper，SQL 全部定义在 resources/mapper/CustomerMapper.xml。
 */
@Mapper
public interface CustomerMapper {

    List<Customer> selectPage(@Param("q") CustomerQuery query);

    long countPage(@Param("q") CustomerQuery query);

    Customer selectById(@Param("id") Long id);

    int insert(Customer customer);

    int update(Customer customer);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);
}
