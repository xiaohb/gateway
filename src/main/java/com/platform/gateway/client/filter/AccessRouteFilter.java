package com.platform.gateway.client.filter;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.platform.gateway.client.constants.GatewayClientConstants;
import com.plt.scf.common.constants.auth.AuthConstant;
import com.plt.scf.common.dto.user.EnterpriserUserInfoDTO;
import com.plt.scf.common.result.ResultCode;
import com.plt.scf.common.util.AccessTokenUtil;
import com.platform.gateway.client.dao.SysClientDAO;
import com.platform.gateway.client.dao.SysServiceDAO;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 访问路由过滤器(对认证客户端client方法服务路径进行鉴权过滤)
 */
@Slf4j
@Component
public class AccessRouteFilter implements GlobalFilter, Ordered {

    // url匹配器
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private  SysServiceDAO  sysServiceDAO;

    @Autowired
    private  SysClientDAO  sysClientDAO;

    @Autowired
    private ScfAuthIgnored scfAuthIgnored;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accessToken = AccessTokenUtil.extractToken(exchange.getRequest());
        String reqUrl = exchange.getRequest().getPath().value();
        log.info("accessRouteFilter accessToken:{},reqUrl:{}",accessToken,reqUrl);
        // 默认
        boolean flag = false;
        for (String ignored : scfAuthIgnored.getIgnored()) {
            if (pathMatcher.match(ignored, exchange.getRequest().getPath().value())) {
                flag = true; // 白名单
            }
        }
        if (flag) {
            return chain.filter(exchange);
        } else {
            if (!hasPermission(accessToken,reqUrl)) {
                log.error("No permission accessToken reqUrl:{}",reqUrl);
                ServerHttpResponse response = exchange.getResponse();
                JSONObject message = new JSONObject();
                message.put("code", ResultCode.NO_PERMISSION.code);
                message.put("message", ResultCode.NO_PERMISSION.msg);
                byte[] bits = message.toJSONString().getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(bits);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                //指定编码，否则在浏览器中会中文乱码
                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                return response.writeWith(Mono.just(buffer));
            }
        }
        return chain.filter(exchange);

    }

    @Override
    public int getOrder() {
        return -502;
    }

    /**
     * 访问用户是否有权限访问
     * @param accessToken  访问用户的令牌
     * @param reqUrl   请求访问的url
     * @return
     */
    private  boolean  hasPermission(String accessToken,String  reqUrl){
        boolean hasPermission = false;
        String redisTokenKey = AuthConstant.TOKEN + ":" + accessToken;
        //从redis获取当前用户详细信息
        String  userDetailInfo = (String)redisTemplate.opsForValue().get(redisTokenKey);
        if (StringUtils.isBlank(userDetailInfo)) {
            log.error("userDetailInfo is null ,redisTokenKey:{}",redisTokenKey);
            return  false;
        }
        EnterpriserUserInfoDTO  userDetailDto = JSONObject.parseObject(userDetailInfo, EnterpriserUserInfoDTO.class);
        if (null != userDetailDto){
            String  clientId = userDetailDto.getClientId();
            if (StringUtils.isBlank(clientId)){
                log.error("clientId is blank from userDetailDto in redis ,userDetailDto:{}",userDetailDto);
                return false;
            }
            Map  sysClientMap = getSysClient(clientId);
            if (null == sysClientMap) {
                log.error("oauth_client_details is not found by clientId:{}",clientId);
                return false;
            } else {
                List<Map>   sysServiceList = getSysService(String.valueOf(sysClientMap.get("id")));
                if (CollectionUtils.isEmpty(sysServiceList)){
                    log.error("sysServiceList is empty by clientId:{}",clientId);
                    return false;
                }
                for (Iterator<Map> it = sysServiceList.iterator(); it.hasNext(); ) {
                    Map sysServiceMap = it.next();
                    if (pathMatcher.match(String.valueOf(sysServiceMap.get("path")),reqUrl)) {
                        log.info("pathMatcher result=true,path:{},reqUrl:{}",String.valueOf(sysServiceMap.get("path")),reqUrl);
                        hasPermission = true;
                        return hasPermission;
                    }
                }
                //all  path  no permission
                if (!hasPermission){
                    log.error("all  path  no permission,clientId:{},reqUrl:{},accessToken:{}",clientId,reqUrl,accessToken);
                }
            }
        } else {
            log.error("userDetailDto is null ,accessToken:{}",accessToken);
        }
        return  hasPermission;
    }

    /**
     * 依据客户端id获取认证客户端详细
     * @param clientId
     * @return
     */
    private  Map  getSysClient(String clientId){
        Map  sysClientMap = null;
        String  oauthClientKey = GatewayClientConstants.OAUTH_CLIENT_ID + clientId;
        //从redis获取认证客户端信息
        String  oauthClient = (String)redisTemplate.opsForValue().get(oauthClientKey);
        if (StringUtils.isNotBlank(oauthClient)){
            sysClientMap  = JSONObject.parseObject(oauthClient, Map.class);
        } else {
            sysClientMap = sysClientDAO.getClient(clientId);
            log.info("select database sysclient by clientId:{},sysClientMap:{}",clientId,(sysClientMap==null?"null":JSON.toJSON(sysClientMap)));
            if (!CollectionUtils.isEmpty(sysClientMap)){
                redisTemplate.opsForValue().set(oauthClientKey,JSONObject.toJSONString(sysClientMap));
            }
        }
        return sysClientMap;
    }

    /**
     * 依据客户端id 获取访问服务列表
     * @param clientId
     * @return
     */
    private  List  getSysService(String  clientId){
        List<Map>   sysServiceList = null;
        String  sysServiceKey = GatewayClientConstants.SYS_SERVER_PATH + clientId;
        //从redis获取认证客户端访问服务列表信息
        String  sysServices = (String)redisTemplate.opsForValue().get(sysServiceKey);
        if (StringUtils.isNotBlank(sysServices)){
            sysServiceList  = JSONObject.parseObject(sysServices, List.class);
        } else {
            sysServiceList = sysServiceDAO.listByClientId(Integer.parseInt(clientId));
            log.info("select database getSysService by clientId:{},sysClientMap:{}",clientId,(sysServiceList==null?"null":JSON.toJSON(sysServiceList)));
            if (!CollectionUtils.isEmpty(sysServiceList)) {
                redisTemplate.opsForValue().set(sysServiceKey, JSONObject.toJSONString(sysServiceList));
            }
        }
        return sysServiceList;
    }
}
