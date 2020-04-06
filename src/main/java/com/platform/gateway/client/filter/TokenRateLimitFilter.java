package com.platform.gateway.client.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.plt.scf.common.constants.auth.AuthConstant;
import com.plt.scf.common.result.Result;
import com.plt.scf.common.result.ResultCode;
import com.plt.scf.common.util.AccessTokenUtil;

import com.platform.gateway.client.utils.RedisLimiterUtils;
import com.platform.gateway.client.vo.ScfAuthIgnored;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 根据应用 url 限流 oauth_client_details if_limit 限流开关
 * limit_count 阈值
 */
@Slf4j
@Component
public class TokenRateLimitFilter implements GlobalFilter, Ordered {
    // url匹配器
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    //默认每天每个AccessToken 访问次数
    private final static int  ACCESS_TOKEN_LIMIT_COUNT_OF_DAY = 10000;
    //默认每小时每个AccessToken 访问次数
    private final static int  ACCESS_TOKEN_LIMIT_COUNT_OF_HOUR = 1000;
    @Autowired
    private StringRedisTemplate redisTemplate;
    
	@Autowired
	private RedisLimiterUtils redisLimiterUtils;


    @Autowired
    private ScfAuthIgnored scfAuthIgnored;

    @Override
    public int getOrder() {
        return -503;
    }

    /**
     * 1. 判断token是否有效
     * 2. 如果token有对应clientId
     * 2.1 判断clientId是否有效
     * 2.2 判断请求的服务service是否有效
     * 2.3 判断clientId是否有权限访问service
     * 3. 判断 clientId+service 每日限流
     * @param exchange
     * @param chain
     * @return
     */

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
       
    	String accessToken = AccessTokenUtil.extractToken(exchange.getRequest());
        log.info("RateLimitFilter accessToken:{}",accessToken);
        if (!checkRateLimit(exchange, accessToken)) {
                log.error("TOO MANY REQUESTS! accessToken:{}",accessToken);
                ServerHttpResponse response = exchange.getResponse();
                JSONObject message = new JSONObject();
                message.put("code", ResultCode.EXCEEDS_LIMIT.code);
                message.put("message", ResultCode.EXCEEDS_LIMIT.msg);
                byte[] bits = message.toJSONString().getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(bits);
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                //指定编码，否则在浏览器中会中文乱码
                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }

    private Boolean checkRateLimit(ServerWebExchange exchange, String accessToken) {
        try {
			String reqUrl = exchange.getRequest().getPath().value();
            log.info("checkRateLimit  reqUrl:{}",reqUrl);
            log.info("checkRateLimit  accessToken:{}",accessToken);
            // 1. 获取redis 中的AccessToken
			String  accesTokenInRedis  = (String)redisTemplate.opsForValue().get(AuthConstant.TOKEN +":"+ accessToken) ;
            log.info("redis key :{},get in redis value :{}",AuthConstant.TOKEN +":"+ accessToken,(accesTokenInRedis == null?"null":accesTokenInRedis));

            if(accesTokenInRedis != null){
				String clientId = scfAuthIgnored.getClientId();
				if(StringUtils.isNotBlank(clientId)) {
				    /**** a.针对访问[每个客户端,访问路径,每天访问次数] 进行限流  *****/
                    String  accessLimitCountOfDay = scfAuthIgnored.getAccessLimitCountOfDay();
                    int  accessLimitCount  = ACCESS_TOKEN_LIMIT_COUNT_OF_DAY;
                    if(StringUtils.isNotBlank(clientId)){
                        accessLimitCount = Integer.parseInt(accessLimitCountOfDay.trim());
                    }
                    if (accessLimitCount > 0) {
                        // 依据访问路径限制每个认证客户端的访问次数(一天)
                        Result result = redisLimiterUtils.rateLimitOfDay(clientId,  reqUrl ,
                                accessLimitCount);
                        if (ResultCode.EXCEEDS_LIMIT.code == result.getCode()) {
                            log.error("access path overrun  of day  accessToken:{},result.message:{}",accessToken,result.getMessage());
                            return false;
                        }
                    } else {
                        //log.error("scfAuthIgnored accessLimitCountOfDay less than 0 , accesTokenInRedis:{}",accesTokenInRedis);
                    }
                    /**** b.针对访问[访问令牌每小时访问次数] 进行限流  *****/
                    String  accessLimitCountOfHour = scfAuthIgnored.getAccessLimitCountOfHour() ;
                    int  intAccessLimitCountOfHour  = ACCESS_TOKEN_LIMIT_COUNT_OF_HOUR;
                    if(StringUtils.isNotBlank(clientId)){
                        intAccessLimitCountOfHour = Integer.parseInt(accessLimitCountOfHour.trim());
                    }
                    if (intAccessLimitCountOfHour > 0) {
                        // 依据用户的访问令牌(accessToken) 限制每个内的访问次数
                        Result result = redisLimiterUtils.accessTokenRateLimiter(clientId,  accessToken, intAccessLimitCountOfHour,1 * 60 * 60);
                        if (ResultCode.EXCEEDS_LIMIT.code == result.getCode()) {
                            log.error("accessToken overrun  of day  accessToken:{},result.message:{}",accessToken,result.getMessage());
                            return false;
                        }
                    } else {
                        //log.error("scfAuthIgnored accessLimitCountOfDay less than 0 , accesTokenInRedis:{}",accesTokenInRedis);
                    }
                    /**  c.其他限流再根据运营进行扩展 (redisLimiterUtils)  **/
				} else {
				    log.error("scfAuthIgnored  clientId is null, accesTokenInRedis:{}",accesTokenInRedis);
                }
			}
		} catch (Exception e) {
            log.error("checkRateLimit exception:{}",e);
			StackTraceElement stackTraceElement= e.getStackTrace()[0];
			log.error("checkRateLimit:" + ",Exception:" +stackTraceElement.getLineNumber()+"----"+ e.getMessage());
		}
        
       return true;
    }


}
