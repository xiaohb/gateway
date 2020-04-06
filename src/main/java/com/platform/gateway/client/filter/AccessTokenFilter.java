package com.platform.gateway.client.filter;

import com.alibaba.fastjson.JSONObject;

import com.platform.gateway.client.redis.RedisUtil;
import com.platform.gateway.client.vo.ScfAuthIgnored;
import com.plt.platform.common.util.SysUserUtil;
import com.plt.scf.common.constants.auth.AuthConstant;

import com.plt.scf.common.dto.user.EnterpriserUserInfoDTO;
import com.plt.scf.common.util.AccessTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 访问令牌过滤器(首次过滤调用,检查请求是否有token,忽略不需要校验的地址)
 */
@Slf4j
@Component
public class AccessTokenFilter implements GlobalFilter, Ordered {

	// url匹配器
	private AntPathMatcher pathMatcher = new AntPathMatcher();

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private ScfAuthIgnored scfAuthIgnored;

	@Override
	public int getOrder() {
		return -505;
	}

	@Autowired
    private RedisUtil redisUtil;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String accessToken = AccessTokenUtil.extractToken(exchange.getRequest());
		log.info("AccessFilter accessToken :{}",accessToken);

		// 默认
		boolean flag = false;

		for (String ignored : scfAuthIgnored.getIgnored()) {

			if (pathMatcher.match(ignored, exchange.getRequest().getPath().value())) {
				flag = true; // 白名单
			}
		}
        log.info("accessFilter flag:{}, req url:{}",flag,exchange.getRequest().getPath().value());
		if (flag) {
			  //TODO  debug  test  token ratelimit
//             if (StringUtils.hasText(accessToken)){
//				 String  value =  (String)redisTemplate.opsForValue().get(AuthConstant.TOKEN +":"+ accessToken);
//				 if (value == null){
//					 redisTemplate.opsForValue().set(AuthConstant.TOKEN +":"+ accessToken,accessToken);
//					 log.info("set accessToken in redis success ,accessToken:{}",accessToken);
//				 } else {
//				 	 log.info(" in redis accessToken :{}",value);
//				 }
//			 }
			return chain.filter(exchange);
		} else {

			String  tokenInRedis = (String) redisTemplate.opsForValue().get(AuthConstant.TOKEN +":"+ accessToken);
			log.info("get tokenInRedis={},token={}",(tokenInRedis==null?"null":tokenInRedis),AuthConstant.TOKEN +":"+ accessToken);
			if (tokenInRedis != null) {
				//2019-11-07 add  由于用户在指定时间内(10分钟)未操作过期,需对有效的token 延长有效期
//				EnterpriserUserInfoDTO  enterpriserUserInfoDTO = SysUserUtil.getUserDetailBy(tokenInRedis);
//				if (null != enterpriserUserInfoDTO) {
//					this.redisTemplate.opsForValue().set(tokenInRedis, JSONObject.toJSONString(enterpriserUserInfoDTO), AuthConstant.TOKEN_TIMEOUT, TimeUnit.MINUTES);
//                    log.info("token in redis ,enterpriserUserInfoDTO:{}",JSONObject.toJSONString(enterpriserUserInfoDTO));
//				}
				boolean expireRs = redisUtil.expire(AuthConstant.TOKEN +":"+ accessToken,AuthConstant.TOKEN_TIMEOUT * 60000);
                log.info("expireRs:{},key:{}",expireRs,(AuthConstant.TOKEN +":"+ accessToken));
				return chain.filter(exchange);
			} else {
				ServerHttpResponse response = exchange.getResponse();
				JSONObject message = new JSONObject();
				message.put("code", 401);
				message.put("message", "未认证通过或令牌失效");
				byte[] bits = message.toJSONString().getBytes(StandardCharsets.UTF_8);
				DataBuffer buffer = response.bufferFactory().wrap(bits);
				response.setStatusCode(HttpStatus.UNAUTHORIZED);
				// 指定编码，否则在浏览器中会中文乱码
				response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
				log.error("accessFilter unauthorized ,accessToken:{}",accessToken);
				return response.writeWith(Mono.just(buffer));
			}

		}

	}

}
