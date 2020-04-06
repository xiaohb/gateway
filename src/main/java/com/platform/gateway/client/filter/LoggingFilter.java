package com.platform.gateway.client.filter;

import com.plt.platform.common.constant.UaaConstant;
import com.plt.platform.common.util.StringUtils;
import com.plt.platform.common.util.SysUserUtil;
import com.plt.scf.common.dto.user.EnterpriserUserInfoDTO;
import com.plt.scf.common.util.AccessTokenUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.stream.Collectors;

/**
 * 记录请求参数及统计执行时长过滤器
 *
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    private static final String START_TIME = "startTime";
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestParams = exchange.getRequest().getQueryParams().toString();
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String method = serverHttpRequest.getMethodValue();
        //2020-03-18 注释掉因为原前端post请求url带有参数,未使用body体提交，为了兼容原前端程序。
//        if ("POST".equals(method.toUpperCase())) {
//            return DataBufferUtils.join(exchange.getRequest().getBody())
//                    .flatMap(dataBuffer -> {
//                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                        dataBuffer.read(bytes);
//                        String  bodyString = "";
//                        try {
//                            bodyString = new String(bytes, "utf-8");
//                            exchange.getAttributes().put("POST_BODY", bodyString);
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                            log.error("UnsupportedEncodingException :{}",e);
//                        }
//                        DataBufferUtils.release(dataBuffer);
//                        Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
//                            DataBuffer buffer = exchange.getResponse().bufferFactory()
//                                    .wrap(bytes);
//                            return Mono.just(buffer);
//                        });
//                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(
//                                exchange.getRequest()) {
//                            @Override
//                            public Flux<DataBuffer> getBody() {
//                                return cachedFlux;
//                            }
//                        };
//
//                        logTrace(exchange,"",bodyString==null?"null":bodyString);
//                        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
//                        return chain.filter(exchange.mutate().request(mutatedRequest).build()).then( Mono.fromRunnable(() -> {
//                            Long startTime = exchange.getAttribute(START_TIME);
//                            if (startTime != null) {
//                                log.trace("request consumeTime:{},path:{}",(System.currentTimeMillis() - startTime),exchange.getRequest().getPath().value());
//                            }
//                        }));
//                    });
//        } else if ("GET".equals(method.toUpperCase())) {

            logTrace(exchange,requestParams,"");

            exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
            return chain.filter(exchange).then( Mono.fromRunnable(() -> {
                Long startTime = exchange.getAttribute(START_TIME);
                if (startTime != null) {
                    log.trace("request consumeTime:{},path:{}",(System.currentTimeMillis() - startTime),exchange.getRequest().getPath().value());
                }
            }));
//        }
//        log.trace("end end end ");
//        return chain.filter(exchange);
    }


    /**
     * 日志信息
     * @param exchange
     * @param requestParam    请求参数
     */
    private void logTrace(ServerWebExchange exchange, String requestParam,String requestBody) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String path = serverHttpRequest.getURI().getPath();
        String method = serverHttpRequest.getMethodValue();
        String headers = serverHttpRequest.getHeaders().entrySet()
                .stream()
                .map(entry -> "   " + entry.getKey() + ": [" + String.join(";", entry.getValue()) + "]")
                .collect(Collectors.joining("\n"));
        String  requestIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        String  hostName = serverHttpRequest.getRemoteAddress().getHostName();

        log.trace("HttpMethod : {}\n" +
                        "Uri       : {}\n" +
                        "requestParam : {}\n" +
                        "requestBody: {}\n" +
                        "Headers   : {}\n" +
                        "currentUserName:{} \n" +
                        "requestIp:{} \n" +
                        "hostName:{} \n"
                , method, path, requestParam,requestBody,headers,getCurrentUserName(exchange),requestIp,hostName);
    }

    private String  getCurrentUserName(ServerWebExchange exchange) {
        String accessToken = AccessTokenUtil.extractToken(exchange.getRequest());
        if (StringUtils.isNoneBlank(accessToken)) {
            EnterpriserUserInfoDTO enterpriserUserInfoDto = SysUserUtil.getUserDetailBy(accessToken);
            if (enterpriserUserInfoDto != null) {
                return  enterpriserUserInfoDto.getUsername();
            }
        }
        return "";
    }

}
