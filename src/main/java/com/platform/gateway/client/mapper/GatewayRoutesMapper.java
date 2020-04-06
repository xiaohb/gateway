package com.platform.gateway.client.mapper;


import com.platform.gateway.client.entity.GatewayRoutesPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface GatewayRoutesMapper {
    int deleteByPrimaryKey(String id);

    int insert(GatewayRoutesPO record);

    int insertSelective(GatewayRoutesPO record);

    GatewayRoutesPO selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(GatewayRoutesPO record);

    int updateByPrimaryKey(GatewayRoutesPO record);

    List<GatewayRoutesPO> findAll(Map map);
}