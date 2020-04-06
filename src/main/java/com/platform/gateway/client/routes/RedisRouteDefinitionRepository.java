package com.platform.gateway.client.routes;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {

    public static final String GATEWAY_ROUTES_PREFIX = "GETEWAY_ROUTES";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Set<RouteDefinition> routeDefinitions = new HashSet<>();

    /**
     * 获取全部路由
     *
     * @return
     */
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        /**
         * 从redis 中 获取 全部路由,因为保存在redis ,mysql 中 频繁读取mysql 有可能会带来不必要的问题
         */

        routeDefinitions.clear();
        log.debug("getRouteDefinitions  boundHashOps  before ");
        BoundHashOperations<String, String, String> boundHashOperations = redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX);
        log.debug("getRouteDefinitions  boundHashOperations :{}", (boundHashOperations == null ? "null" : boundHashOperations));
        Map<String, String> map = null;
        try {
             map = boundHashOperations.entries();
             log.debug("getRouteDefinitions  boundHashOperations  map:{}", (map == null ? "null" : map.size()));
        } catch (RedisSystemException ex){
            ex.printStackTrace();
            log.error("get routeDefinitions from redis exception:{}",ex);
            return Flux.fromIterable(routeDefinitions);
        }

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            routeDefinitions.add(JSON.parseObject(entry.getValue(), RouteDefinition.class));

        }


        return Flux.fromIterable(routeDefinitions);
    }

    /**
     * 添加路由方法
     *
     * @param route
     * @return
     */
    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(routeDefinition -> {
            routeDefinitions.add(routeDefinition);
            return Mono.empty();
        });
    }

    /**
     * 删除路由
     *
     * @param routeId
     * @return
     */
    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            List<RouteDefinition> collect = routeDefinitions.stream().filter(
                    routeDefinition -> StringUtils.equals(routeDefinition.getId(), id)
            ).collect(Collectors.toList());
            routeDefinitions.removeAll(collect);
            return Mono.empty();
        });
    }
}
