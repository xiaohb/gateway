package com.platform.gateway.client.service;



import com.plt.platform.common.web.PageResult;
import com.platform.gateway.client.dto.GatewayRouteDefinitionDTO;
import com.platform.gateway.client.vo.GatewayRoutesVO;

import java.util.Map;

/**
 * 操作 路由 Service
 */
public interface DynamicRouteService {

    /**
     * 新增路由
     * @param gatewayRouteDefinitionDTO
     * @return
     */
    String add(GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO);

    /**
     * 修改路由
      * @param gatewayRouteDefinitionDTO
     * @return
     */
    String update(GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO);



    /**
     * 删除路由
     * @param id
     * @return
     */
    String delete(String id);


    /**
     * 查询全部数据
     * @return
     */
    PageResult<GatewayRoutesVO> findAll(Map<String, Object> params);

    /**
     *  同步redis数据 从mysql中同步过去
     *
     * @return
     */
    String synchronization();


    /**
     * 更改路由状态
     * @param params
     * @return
     */
    String updateFlag(Map<String, Object> params);


}
