package com.platform.gateway.client.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由模型
 */
@EqualsAndHashCode
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayRouteDefinitionDTO {

    //路由的Id
    private String id;
    //路由断言集合配置
    private List<GatewayPredicateDefinitionDTO> predicates = new ArrayList<>();
    //路由过滤器集合配置
    private List<GatewayFilterDefinitionDTO> filters = new ArrayList<>();
    //路由规则转发的目标uri
    private String uri;
    //路由执行的顺序
    private int order = 0;
    //路由描述
    private String description;
}
