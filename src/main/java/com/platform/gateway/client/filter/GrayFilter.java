//package com.plt.scf.gateway.client.filter;
//
//
//
//
//import com.plt.platform.ribbon.support.RibbonFilterContextHolder;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//
//import org.springframework.util.CollectionUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import java.util.List;
//import com.plt.scf.common.constants.gray.GrayContstant;
//
//
///**
// *  灰度环境访问分发过滤器
// *  head头或请求参数中带有[gray_mark=enable] 访问路由到灰度环境服务
// */
//@Slf4j
//@Component
//public class GrayFilter  implements GlobalFilter, Ordered {
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        log.info("inner GrayFilter filter");
//        //灰度标记
//        List<String> strings = exchange.getRequest().getHeaders().get(GrayContstant.GRAY_MARK_TAG);
//        String  grayMarkValue  = null;
//        if (!CollectionUtils.isEmpty(strings)) {
//            grayMarkValue = ((String)strings.get(0)).trim();
//        }
//        if (StringUtils.isBlank(grayMarkValue)) {
//            strings =  exchange.getRequest().getQueryParams().get(GrayContstant.GRAY_MARK_TAG);
//            if (strings != null) {
//                grayMarkValue = (String)strings.get(0).trim();
//            }
//        }
//        log.info("grayMarkValue:{}",grayMarkValue);
//
//        if (StringUtils.isNotBlank(grayMarkValue) && GrayContstant.GRAY_MARK_VALUE.equals(grayMarkValue)) {
//            log.info("gray_mark:{}",GrayContstant.GRAY_HOST);
//            RibbonFilterContextHolder.getCurrentContext().add(GrayContstant.HOST_MARK, GrayContstant.GRAY_HOST);
//        } else {
//            log.info("gray_mark:{}",GrayContstant.RUNNING_HOST);
//            RibbonFilterContextHolder.getCurrentContext().add(GrayContstant.HOST_MARK, GrayContstant.RUNNING_HOST);
//        }
//        //灰度版本
//        List<String> versionStrings = exchange.getRequest().getHeaders().get(GrayContstant.GRAY_VERSION);
//        String  grayVersionValue  = null;
//        if (!CollectionUtils.isEmpty(versionStrings)) {
//            grayVersionValue = ((String)versionStrings.get(0)).trim();
//        }
//        if (StringUtils.isBlank(grayVersionValue)) {
//            versionStrings =  exchange.getRequest().getQueryParams().get(GrayContstant.GRAY_VERSION);
//            if (versionStrings != null) {
//                grayVersionValue = (String)versionStrings.get(0).trim();
//            }
//        }
//        log.info("grayVersionValue:{}",grayVersionValue);
//
//        if (StringUtils.isNotBlank(grayVersionValue) ) {
//            log.info("set grayVersionValue:{}",grayVersionValue);
//            RibbonFilterContextHolder.getCurrentContext().add(GrayContstant.GRAY_VERSION, grayVersionValue);
//        }
//
//        return chain.filter(exchange);
//    }
//
//    @Override
//    public int getOrder() {
//        return  -100;
//    }
//
//}
