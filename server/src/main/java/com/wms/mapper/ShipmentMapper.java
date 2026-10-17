package com.wms.mapper;

import com.wms.entity.Shipment;
import com.wms.vo.ShipmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发货单 Mapper，SQL 全部定义在 resources/mapper/ShipmentMapper.xml。
 */
@Mapper
public interface ShipmentMapper {

    int insert(Shipment shipment);

    List<ShipmentVO> selectByOutboundId(@Param("outboundId") Long outboundId);
}
