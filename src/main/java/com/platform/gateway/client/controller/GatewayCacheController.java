package com.platform.gateway.client.controller;


import com.platform.gateway.client.constants.GatewayClientConstants;
import com.plt.scf.common.result.Result;
import com.plt.scf.common.result.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;


/**
 * 网关缓存清除
 */
@RestController
@RequestMapping("/gatewaycache")
@Slf4j
public class GatewayCacheController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 清除网关客户端redis缓存内容
     * @param clientCode
     * @return
     */
    @GetMapping("/cleanClientId")
    public Result cleanClientId(@RequestParam  String   clientCode) {
        if (StringUtils.isNotBlank(clientCode)){
            String  oauthClietIdKey = GatewayClientConstants.OAUTH_CLIENT_ID + clientCode;
            boolean delResult = redisTemplate.delete(oauthClietIdKey);
            log.info("cleanClientId delResult:{},oauthClietIdKey:{}",delResult,oauthClietIdKey);
            return ResultGenerator.genSuccessResult(delResult);
        } else {
           return ResultGenerator.genFailResult("clientCode is blank");
        }
    }


    /**
     * 清除网关客户端访问服务redis缓存内容
     * @param clientId
     * @return
     */
    @GetMapping("/cleanSysService")
    public Result cleanSysService(@RequestParam  String   clientId) {
        if (StringUtils.isNotBlank(clientId)){
            String  sysServiceKey = GatewayClientConstants.SYS_SERVER_PATH + clientId;
            boolean delResult = redisTemplate.delete(sysServiceKey);
            log.info("cleanSysService delResult:{},oauthClietIdKey:{}",delResult,sysServiceKey);
            return ResultGenerator.genSuccessResult(delResult);
        } else {
            return ResultGenerator.genFailResult("clientId is blank");
        }
    }
}
