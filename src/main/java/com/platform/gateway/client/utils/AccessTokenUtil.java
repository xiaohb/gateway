//package com.plt.scf.gateway.client.utils;
//
//
//import com.plt.scf.common.constants.auth.AuthConstant;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//
//import java.util.List;
//
//public class AccessTokenUtil {
//	public  static String extractToken(ServerHttpRequest request) {
//		List<String> strings = request.getHeaders().get(AuthConstant.Authorization);
//		String authToken = null;
//		if (strings != null) {
//			authToken = strings.get(0).substring("Bearer".length()).trim();
//		}
//
//		if (StringUtils.isBlank(authToken)) {
//			strings = request.getQueryParams().get(AuthConstant.TOKEN_PARAM);
//			if (strings != null) {
//				authToken = strings.get(0);
//			}
//		}
//
//		return authToken;
//	}
//}
