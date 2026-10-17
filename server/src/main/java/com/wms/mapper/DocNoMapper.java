package com.wms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 单据号生成支撑 Mapper，SQL 全部定义在 resources/mapper/DocNoMapper.xml。
 * 不新增任何表：用事务级 advisory lock 串行化同前缀的单号分配。
 */
@Mapper
public interface DocNoMapper {

    /** 取事务级排他锁，必须在同一事务内调用（pg_advisory_xact_lock） */
    void lockDocNo(@Param("key") String key);

    /**
     * 取当天已分配的最大序号。
     * table / column 由 Java 侧白名单校验后传入，不接受外部输入。
     */
    int selectMaxSeq(@Param("table") String table,
                     @Param("column") String column,
                     @Param("digits") int digits,
                     @Param("likePrefix") String likePrefix);
}
