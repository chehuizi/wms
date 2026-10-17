package com.wms.mapper;

import com.wms.dto.WaveQuery;
import com.wms.entity.Wave;
import com.wms.vo.WaveVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拣货波次 Mapper（含波次-订单关联），SQL 定义在 resources/mapper/WaveMapper.xml。
 */
@Mapper
public interface WaveMapper {

    List<WaveVO> selectPage(@Param("q") WaveQuery query);

    long countPage(@Param("q") WaveQuery query);

    Wave selectById(@Param("id") Long id);

    /** 释放波次前加悲观锁 */
    Wave selectForUpdate(@Param("id") Long id);

    WaveVO selectVOById(@Param("id") Long id);

    int insert(Wave wave);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markReleased(@Param("id") Long id, @Param("operator") String operator);

    int markFinished(@Param("id") Long id);

    int insertWaveOrder(@Param("waveId") Long waveId, @Param("outboundId") Long outboundId);

    /** 按关联表回写波次包含的订单数 */
    int refreshOrderCount(@Param("waveId") Long waveId);

    List<Long> selectWaveOutboundIds(@Param("waveId") Long waveId);

    int countWaveOutbound(@Param("waveId") Long waveId);

    /** 该波次下未完成的拣货任务数 */
    int countUnfinishedPickTasks(@Param("waveId") Long waveId);
}
