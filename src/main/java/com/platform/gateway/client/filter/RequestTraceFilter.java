package com.platform.gateway.client.filter;


import com.plt.platform.common.util.SnowUtil;
import com.plt.scf.common.constants.auth.AuthConstant;
import com.plt.scf.common.constants.log.TraceConstant;
import com.plt.scf.common.util.AccessTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *  模块 : 网关
 *  描述 : 请求Header头加入LOG_TRACE_ID 日志跟踪
 */
@Slf4j
@Component
public class RequestTraceFilter implements GlobalFilter, Ordered {
    //request url
//    private  SnowUtil  snowUtil = new SnowUtil();
	@Override
	public int getOrder() {
		return -504;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		 
		String traceId = MDC.get(TraceConstant.LOG_B3_TRACEID);
		//String  traceId = ""+snowUtil.nextId();
		MDC.put(TraceConstant.LOG_TRACE_ID, traceId);
		String accessToken = AccessTokenUtil.extractToken(exchange.getRequest());
        //构建head
		ServerHttpRequest traceHead = null;
//		if (StringUtils.isNotBlank(accessToken)) {
//			traceHead = exchange.getRequest().mutate().header(TraceConstant.HTTP_HEADER_TRACE_ID, traceId)
//					.header(AuthConstant.TOKEN_HEADER, accessToken).build();
//		} else {
//			traceHead = exchange.getRequest().mutate().header(TraceConstant.HTTP_HEADER_TRACE_ID, traceId).build();
//		}
		if (StringUtils.isNotBlank(traceId)){
			traceHead = exchange.getRequest().mutate().header(TraceConstant.HTTP_HEADER_TRACE_ID, traceId).build();
		}
		//将现在的request 变成 change对象
        log.info("request accessToken:{},url:{},traceId：{} ",accessToken,exchange.getRequest().getURI(),traceId);
			ServerWebExchange build = exchange.mutate().request(traceHead).build();
			return chain.filter(build);
	}
}
