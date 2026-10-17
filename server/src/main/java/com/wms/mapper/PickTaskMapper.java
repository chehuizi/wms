package com.wms.mapper;

import com.wms.dto.PickTaskQuery;
import com.wms.entity.PickTask;
import com.wms.vo.PickTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拣货任务 Mapper，SQL 全部定义在 resources/mapper/PickTaskMapper.xml。
 */
@Mapper
public interface PickTaskMapper {

    List<PickTaskVO> selectPage(@Param("q") PickTaskQuery query);

    long countPage(@Param("q") PickTaskQuery query);

    PickTask selectById(@Param("id") Long id);

    /** 单条详情（含波次号 / 订单号 / 库位编码） */
    PickTaskVO selectVOById(@Param("id") Long id);

    /** 拣货前加悲观锁 */
    PickTask selectForUpdate(@Param("id") Long id);

    int insert(PickTask task);

    /** 累加已拣数量；返回 0 表示已拣满，调用方据此判定任务完成 */
    int addPickedQty(@Param("id") Long id, @Param("delta") BigDecimal delta);

    int markStatus(@Param("id") Long id,
                   @Param("status") Integer status,
                   @Param("operator") String operator);

    /** 释放分配时，把该 allocation 下未完成的拣货任务置为已取消 */
    int cancelUnfinishedByAllocation(@Param("allocationId") Long allocationId);

    /** 该订单下未完成的拣货任务数 */
    int countUnfinishedByOutbound(@Param("outboundId") Long outboundId);
}
