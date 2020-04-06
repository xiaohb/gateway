package com.platform.gateway.client;


import com.platform.gateway.client.vo.ScfAuthIgnored;
import com.plt.platform.common.util.SpringBeanTool;
import com.plt.scf.common.constants.auth.AuthConstant;
import com.platform.gateway.client.filter.RateLimitByIpGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import reactor.core.publisher.Mono;

import java.time.Duration;


/**
 * 平台网关
 */

@EnableCircuitBreaker
@SpringBootApplication(scanBasePackages = {"com.plt.scf"})
@EnableDiscoveryClient
@EnableConfigurationProperties(value = ScfAuthIgnored.class)
public class ScfGatewayApp {
    public static void main(String[] args) {
        ApplicationContext app = SpringApplication.run(ScfGatewayApp.class, args);
        SpringBeanTool.setApplicationContext(app);
    }

    public static void exitApplication(ConfigurableApplicationContext context) {
        int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
        System.exit(exitCode);
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(2, 4);
    }

    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst(AuthConstant.TOKEN_PARAM));
    }
    /** 基于路由限流:使用springcloud gateway 自带的 RedisRateLimiter   end  **/
    /***********   基于服务器使用率限流实例 start ************/

//    @Autowired
//    private RateLimitByCpuGatewayFilter rateLimitByCpuGatewayFilter;
//
//    @Bean
//    public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
//        // @formatter:off
//        return builder.routes()
//                .route(r -> r.path("/**")
//                        .filters(f -> f.stripPrefix(2)
//                                .filter(rateLimitByCpuGatewayFilter))
//                        .uri("lb://CONSUMER")
//                        .order(0)
//                        .id("throttle_customer_service")
//                )
//                .build();
//        // @formatter:on
//    }

    /***********   基于服务器使用率限流实例 end ************/

    /***********   基于IP 地址使用令牌桶限流实例 start ************/

    @Autowired
    private RateLimitByIpGatewayFilter rateLimitByIpGatewayFilter;

    @Bean
    public RouteLocator userRouteLocator(RouteLocatorBuilder builder) {
        // @formatter:off
        return builder.routes()
                //0. 测试百度
                .route("path_route", r -> r.path("/baidu/**")
                        .filters(f -> f.addRequestHeader("hello", "world")
                                .addRequestParameter("name", "zhangsan")
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
                        .uri("https://www.baidu.com/cache/sethelp/help.html"))
                //1.1  用户中心(API)
                .route(r -> r.path("/scf/user-api/**")
                        .filters(f -> f.stripPrefix(2)
                                 .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                 .rewritePath("(?<segment>.*)", "${segment}")
                         )

                        .uri("lb://scf-user-business")
                        .order(101)
                        .id("scf-user-business")
                        //.predicate(s -> s.getRequest().getHeaders().get("gray_mark").equals("enable"))
                )
//                //1.2  用户中心(API) 灰度环境
//                .route( r-> r.query("gray_mark","enable").or().header("gray_mark","enable").and().path("/scf/user-api/**")
//                        .filters(f -> f.stripPrefix(2)
//                        //.filter(new RateLimitByIpGatewayFilter(10, 1, Duration.ofSeconds(1)))
//                        .rewritePath("(?<segment>.*)", "${segment}")
//                        )
//                        .uri("lb://scf-user-business-gray")
//                        .order(11)
//                        .id("scf-user-business-gray")
//                )
                //2.1 用户中心(manager)
                .route(r -> r.path("/scf/user-admin/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-user-manager")
                        .order(102)
                        .id("scf-user-manager")
                )
                //3.1 认证中心
                .route(r -> r.path("/scf/oauth/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-oauth")
                        .order(103)
                        .id("scf-oauth")
                )

                //4.1 账户服务(scf-account-business)
                .route(r -> r.path("/scf/account-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-account-business")
                        .order(104)
                        .id("scf-account-business")
                )
                //5.1 账户服务(scf-account-manager)
                .route(r -> r.path("/scf/account-admin/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-account-manager")
                        .order(105)
                        .id("scf-account-manager")
                )
                //6.1 基础数据服务(scf-basedata-business)
                .route(r -> r.path("/scf/basedata-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-basedata-business")
                        .order(106)
                        .id("scf-basedata-business")
                )
                //7.1. 合同业务服务(scf-contract-business)
                .route(r -> r.path("/scf/contract-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-contract-business")
                        .order(107)
                        .id("scf-contract-business")
                )
                //8.1  合同业务服务(scf-contract-manager)
                .route(r -> r.path("/scf/contract-admin/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-contract-manager")
                        .order(108)
                        .id("scf-contract-manager")
                )
                //9.1. 活体服务 (scf-intelligence-recognition)
                .route(r -> r.path("/scf/intel-rec/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-intelligence-recognition")
                        .order(109)
                        .id("scf-intelligence-recognition")
                )
                //10.1. 通知服务 (scf-message-business)
                .route(r -> r.path("/scf/message-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-message-business")
                        .order(110)
                        .id("scf-message-business")
                )
                //11.1 通知服务 (scf-message-manager)
                .route(r -> r.path("/scf/message-admin/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-message-manager")
                        .order(111)
                        .id("scf-message-manager")
                )
                //12.1 广告推送 (scf-cms)
                .route(r -> r.path("/scf/cms/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-cms")
                        .order(112)
                        .id("scf-cms")
                )
                //13.1 供金业务 (scf-business-api)
                .route(r -> r.path("/scf/buiness-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-business-api")
                        .order(113)
                        .id("scf-business-api")
                )
                //14.1 供金业务 (scf-business-manage)
                .route(r -> r.path("/scf/buiness-admin/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-business-manage")
                        .order(114)
                        .id("scf-business-manage")
                )
                //15.1 商品类目 (scf-sku-api)
                .route(r -> r.path("/scf/sku-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-sku-api")
                        .order(115)
                        .id("scf-sku-api")
                )
                //16.1 文件基础服务 (scf-fileupload-business)
                .route(r -> r.path("/scf/fileupload-api/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-fileupload-business")
                        .order(116)
                        .id("scf-fileupload-business")
                )
                //17.1 官网模板 (scf-upgrade)
                .route(r -> r.path("/scf/upgrade/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-upgrade")
                        .order(117)
                        .id("scf-upgrade")
                )
                //18 商品信息价格搜索 (scf-es-productinfo)
                .route(r -> r.path("/scf/es-productInfo/**")
                        .filters(f -> f.stripPrefix(2)
                                .filter(new RateLimitByIpGatewayFilter(20, 5, Duration.ofSeconds(1)))
                                .rewritePath("(?<segment>.*)", "${segment}")
                        )
                        .uri("lb://scf-es-productinfo")
                        .order(118)
                        .id("scf-es-productinfo")
                )
                .build();
        // @formatter:on
    }
    /***********   基于IP 地址使用令牌桶限流实例 end ************/
}
