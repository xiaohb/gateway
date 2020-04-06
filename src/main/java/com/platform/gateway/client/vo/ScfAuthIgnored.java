package com.platform.gateway.client.vo;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * url白名单处理 application.yml中配置需要放权的url白名单
 * @version 创建时间：2017年11月12日 上午22:57:51
 */
@Slf4j
@ConfigurationProperties(prefix = "auth")
public class ScfAuthIgnored {

	/**
	 * 监控中心和swagger需要访问的url
	 */
	private static final String[] ENDPOINTS = { 
			"/**/actuator/**" , "/**/actuator/**/**" ,  //断点监控
			"/**/v2/api-docs/**", "/**/swagger-ui.html", "/**/swagger-resources/**", "/**/webjars/**", //swagger
			"/**/turbine.stream","/**/turbine.stream**/**", "/**/hystrix", "/**/hystrix.stream", "/**/hystrix/**", "/**/hystrix/**/**",	"/**/proxy.stream/**" , //熔断监控
			"/**/druid/**", "/favicon.ico","/**/favicon.ico", "/**/prometheus"
	};

	private String[] ignored;
	//认证客户端Id
    private String  clientId;
    //认证后访问每天限制数量
    private String  accessLimitCountOfDay;
    //认证后每小时访问限制(依据访问令牌)
	private String  accessLimitCountOfHour;
	/**
	 * 需要放开权限的url
	 *
	 *
	 *            自定义的url
	 * @return 自定义的url和监控中心需要访问的url集合
	 */
	public String[] getIgnored() {
        log.debug("getIgnored  :{}",JSON.toJSON(ignored));
		if (ignored == null || ignored.length == 0) {
			return ENDPOINTS;
		}

		List<String> list = new ArrayList<>();
		for (String url : ENDPOINTS) {
			list.add(url);
		}
		for (String url : ignored) {
			list.add(url);
		}

		return list.toArray(new String[list.size()]);
	}

	public void setIgnored(String[] ignored) {
		log.debug("setIgnored  :{}",JSON.toJSON(ignored));
		this.ignored = ignored;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAccessLimitCountOfDay() {
		return accessLimitCountOfDay;
	}

	public void setAccessLimitCountOfDay(String accessLimitCountOfDay) {
		this.accessLimitCountOfDay = accessLimitCountOfDay;
	}

	public String getAccessLimitCountOfHour() {
		return accessLimitCountOfHour;
	}

	public void setAccessLimitCountOfHour(String accessLimitCountOfHour) {
		this.accessLimitCountOfHour = accessLimitCountOfHour;
	}
}
