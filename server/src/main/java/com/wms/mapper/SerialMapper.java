package com.wms.mapper;

import com.wms.dto.SerialQuery;
import com.wms.entity.Serial;
import com.wms.vo.SerialVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 序列号台账 Mapper，SQL 全部定义在 resources/mapper/SerialMapper.xml。
 */
@Mapper
public interface SerialMapper {

    List<SerialVO> selectPage(@Param("q") SerialQuery query);

    long countPage(@Param("q") SerialQuery query);

    SerialVO selectVOById(@Param("id") Long id);

    Serial selectBySerialNo(@Param("serialNo") String serialNo);

    int insert(Serial serial);

    /** 移库时跟随货物移动 */
    int updateLocation(@Param("id") Long id,
                       @Param("warehouseId") Long warehouseId,
                       @Param("locationId") Long locationId,
                       @Param("status") Integer status);

    /** 出库：置 3 已出库并写 outBoundAt */
    int markOutbound(@Param("id") Long id);

    /** 分配时置 2 已锁定 / 释放时恢复 1 在库 */
    int markStatus(@Param("id") Long id, @Param("status") Integer status);
}
