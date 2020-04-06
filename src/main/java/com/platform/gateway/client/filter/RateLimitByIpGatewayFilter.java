package com.platform.gateway.client.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于IP 使用令牌桶算法限流过滤器
 */
@Slf4j
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class RateLimitByIpGatewayFilter implements GatewayFilter, Ordered {

        int capacity;
        int refillTokens;
        Duration refillDuration;

        private static final Map<String, Bucket> CACHE = new ConcurrentHashMap<>();

        private Bucket createNewBucket() {
            Refill refill = Refill.of(refillTokens,refillDuration);
            Bandwidth limit = Bandwidth.classic(capacity,refill);
            return Bucket4j.builder().addLimit(limit).build();
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                 if (!enableRateLimit){
//                     return chain.filter(exchange);
//                 }
                String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                Bucket bucket = CACHE.computeIfAbsent(ip,k -> createNewBucket());
                log.debug("IP: " + ip + "，TokenBucket Available Tokens: " + bucket.getAvailableTokens());
                if (bucket.tryConsume(1)) {
                    return chain.filter(exchange);
                } else {
                    log.error("too many requests ,url:{},ip:{}",exchange.getRequest().getPath().value(),ip);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
        }

        @Override
        public int getOrder() {
            return -1000;
        }

}
