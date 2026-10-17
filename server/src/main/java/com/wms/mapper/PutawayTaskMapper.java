package com.wms.mapper;

import com.wms.dto.PutawayTaskQuery;
import com.wms.entity.PutawayTask;
import com.wms.vo.PutawayTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 上架任务 Mapper，SQL 全部定义在 resources/mapper/PutawayTaskMapper.xml。
 */
@Mapper
public interface PutawayTaskMapper {

    List<PutawayTaskVO> selectPage(@Param("q") PutawayTaskQuery query);

    long countPage(@Param("q") PutawayTaskQuery query);

    PutawayTask selectById(@Param("id") Long id);

    /** 单条详情（含关联名称） */
    PutawayTaskVO selectVOById(@Param("id") Long id);

    /** 执行前加悲观锁 */
    PutawayTask selectForUpdate(@Param("id") Long id);

    int insert(PutawayTask task);

    int markCompleted(@Param("id") Long id,
                     @Param("toLocationId") Long toLocationId,
                     @Param("operator") String operator);

    int markCancelled(@Param("id") Long id);
}
