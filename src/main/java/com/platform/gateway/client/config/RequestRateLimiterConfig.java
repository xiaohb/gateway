package com.platform.gateway.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 定义spring cloud gateway中的  key-resolver: "#{@ipAddressKeyResolver}" #SPEL表达式去的对应的bean
 *  ipAddressKeyResolver 要取bean的名字
 *
 */
@Configuration
public class RequestRateLimiterConfig   {

    /**
     * 根据 HostName 进行限流
     * @return
     */
    @Bean
    @Primary
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

    /**
     * 根据api接口来限流
     * @return
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange ->  Mono.just(exchange.getRequest().getPath().value());
    }

    /**
     * 用户限流
     * 使用这种方式限流，请求路径中必须携带userId参数。
     *  提供第三种方式
     * @return
     */
    @Bean
    public  KeyResolver userKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("userId"));
    }


}
