package com.platform.gateway.client.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.platform.gateway.client.dto.GatewayFilterDefinitionDTO;
import com.platform.gateway.client.dto.GatewayPredicateDefinitionDTO;
import com.platform.gateway.client.dto.GatewayRouteDefinitionDTO;
import com.platform.gateway.client.entity.GatewayRoutesPO;
import com.platform.gateway.client.mapper.GatewayRoutesMapper;
import com.platform.gateway.client.service.DynamicRouteService;
import com.platform.gateway.client.vo.GatewayRoutesVO;
import com.plt.platform.common.web.PageResult;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.*;

import static com.platform.gateway.client.routes.RedisRouteDefinitionRepository.GATEWAY_ROUTES_PREFIX;

@Service
public class DynamicRouteServiceImpl implements ApplicationEventPublisherAware, DynamicRouteService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private RouteDefinitionWriter routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    @Autowired
    private GatewayRoutesMapper gatewayRoutesMapper;


    /**
     * 初始化 转化对象
     */
    private static MapperFacade routeDefinitionMapper;
    private static MapperFacade routeVOMapper;
    static {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        mapperFactory.classMap(GatewayRouteDefinitionDTO.class, GatewayRoutesPO.class)
                .exclude("filters")
                .exclude("predicates")
                .byDefault();
        routeDefinitionMapper = mapperFactory.getMapperFacade();

        //  routeVOMapper
        mapperFactory.classMap(GatewayRoutesPO.class, GatewayRoutesVO.class)
                .byDefault();
        routeVOMapper = mapperFactory.getMapperFacade();

    }


    /**
     *  给spring注册事件
     *      刷新路由
     */
    private void notifyChanged() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * Set the ApplicationEventPublisher that this object runs in.
     * <p>Invoked after population of normal bean properties but before an init
     * callback like InitializingBean's afterPropertiesSet or a custom init-method.
     * Invoked before ApplicationContextAware's setApplicationContext.
     *
     * @param applicationEventPublisher event publisher to be used by this object
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

//    @Override
//    public String add(RouteDefinition definition) {
//        redisTemplate.opsForValue().set(GATEWAY_ROUTES_PREFIX + definition.getId(), JSONObject.toJSONString(definition));
//        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
//        notifyChanged();
//        return "success";
//    }
//
//    @Override
//    public String update(RouteDefinition definition) {
//        redisTemplate.delete(GATEWAY_ROUTES_PREFIX + definition.getId());
//        redisTemplate.opsForValue().set(GATEWAY_ROUTES_PREFIX + definition.getId(), JSONObject.toJSONString(definition));
//        return "success";
//        try {
//            this.routeDefinitionWriter.delete(Mono.just(definition.getId()));
//        } catch (Exception e) {
//            return "update fail,not find route  routeId: " + definition.getId();
//        }
//        try {
//            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
//            notifyChanged();
//            return "success";
//        } catch (Exception e) {
//            return "update route  fail";
//        }
//    }




    /**
     * 新增路由
     *
     * @param gatewayRouteDefinitionDTO
     * @return
     */
    @Override
    public String add(GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO) {
        GatewayRoutesPO gatewayRoutesPO = transformToGatewayRoutes(gatewayRouteDefinitionDTO);
        gatewayRoutesPO.setDelFlag(0);
        gatewayRoutesPO.setCreateTime(new Date());
        gatewayRoutesPO.setUpdateTime(new Date());
        gatewayRoutesMapper.insertSelective(gatewayRoutesPO);

        gatewayRouteDefinitionDTO.setId(gatewayRoutesPO.getId());
       
        redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).put(gatewayRouteDefinitionDTO.getId(),  JSONObject.toJSONString(gatewayRouteDefinitionDTO));
        
        
        return gatewayRoutesPO.getId();
    }

    /**
     * 修改路由
     *
     * @param gatewayRouteDefinitionDTO
     * @return
     */
    @Override
    public String update(GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO) {
        GatewayRoutesPO gatewayRoutesPO = transformToGatewayRoutes(gatewayRouteDefinitionDTO);
        gatewayRoutesPO.setCreateTime(new Date());
        gatewayRoutesPO.setUpdateTime(new Date());
        gatewayRoutesMapper.updateByPrimaryKeySelective(gatewayRoutesPO);

        
    	redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).delete(gatewayRouteDefinitionDTO.getId());
        
        redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).put(gatewayRouteDefinitionDTO.getId(),  JSONObject.toJSONString(gatewayRouteDefinitionDTO));
        
        
        return gatewayRouteDefinitionDTO.getId();
    }


    /**
     * 删除路由
     * @param id
     * @return
     */
    @Override
    public String delete(String id) {
        gatewayRoutesMapper.deleteByPrimaryKey(id);
        redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).delete( id ); 
        
        return "success";
//        try {
//            this.routeDefinitionWriter.delete(Mono.just(id));
//            notifyChanged();
//            return "delete success";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "delete fail";
//        }
    }

    /**
     * 查询全部数据
     *
     * @return
     */
    @Override
    public PageResult<GatewayRoutesVO> findAll(Map<String, Object> params) {
        PageHelper.startPage(MapUtils.getInteger(params, "page"),MapUtils.getInteger(params, "limit"),true);
        List<GatewayRoutesPO> alls = gatewayRoutesMapper.findAll(new HashMap());
        PageInfo<GatewayRoutesVO> pageInfo = new PageInfo<>(routeVOMapper.mapAsList(alls, GatewayRoutesVO.class));
        return PageResult.<GatewayRoutesVO>builder().data(pageInfo.getList()).code(0).count(pageInfo.getTotal()).build();
    }

    /**
     * @return
     */
    @Override
    public String synchronization() {
        HashMap map = new HashMap();
        map.put("delFlag", 0);
        List<GatewayRoutesPO> alls = gatewayRoutesMapper.findAll(map);

        for (GatewayRoutesPO route:   alls) {
            GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO = GatewayRouteDefinitionDTO.builder()
                    .description(route.getDescription())
                    .id(route.getId())
                    .order(route.getOrder())
                    .uri(route.getUri())
                    .build();

            List<GatewayFilterDefinitionDTO> gatewayFilterDefinitionDTOS = JSONArray.parseArray(route.getFilters(), GatewayFilterDefinitionDTO.class);
            List<GatewayPredicateDefinitionDTO> gatewayPredicateDefinitionDTOS = JSONArray.parseArray(route.getPredicates(), GatewayPredicateDefinitionDTO.class);
            gatewayRouteDefinitionDTO.setPredicates(gatewayPredicateDefinitionDTOS);
            gatewayRouteDefinitionDTO.setFilters(gatewayFilterDefinitionDTOS);

            redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).put( route.getId() ,  JSONObject.toJSONString(gatewayRouteDefinitionDTO));
            
            
        }

        return "success";
    }

    /**
     * 更改路由状态
     *
     * @param params
     * @return
     */
    @Override
    public String updateFlag(Map<String, Object> params) {
        String id = MapUtils.getString(params, "id");
        Integer flag = MapUtils.getInteger(params, "flag");

        GatewayRoutesPO gatewayRoutesPO = gatewayRoutesMapper.selectByPrimaryKey(id);
        if (gatewayRoutesPO == null) {
            return "路由不存在";
        }

        if (flag == 1){
            redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).delete( id ); 
            
        }else {
            GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO = GatewayRouteDefinitionDTO.builder()
                    .description(gatewayRoutesPO.getDescription())
                    .id(gatewayRoutesPO.getId())
                    .order(gatewayRoutesPO.getOrder())
                    .uri(gatewayRoutesPO.getUri())
                    .build();

            List<GatewayFilterDefinitionDTO> gatewayFilterDefinitionDTOS = JSONArray.parseArray(gatewayRoutesPO.getFilters(), GatewayFilterDefinitionDTO.class);
            List<GatewayPredicateDefinitionDTO> gatewayPredicateDefinitionDTOS = JSONArray.parseArray(gatewayRoutesPO.getPredicates(), GatewayPredicateDefinitionDTO.class);
            gatewayRouteDefinitionDTO.setPredicates(gatewayPredicateDefinitionDTOS);
            gatewayRouteDefinitionDTO.setFilters(gatewayFilterDefinitionDTOS);

            redisTemplate.boundHashOps(GATEWAY_ROUTES_PREFIX).put( gatewayRoutesPO.getId() ,  JSONObject.toJSONString(gatewayRouteDefinitionDTO));
            
        }

        gatewayRoutesPO.setDelFlag(flag);
        gatewayRoutesPO.setUpdateTime(new Date());
        int i = gatewayRoutesMapper.updateByPrimaryKeySelective(gatewayRoutesPO);
        return i > 0 ? "更新成功": "更新失败";
    }

    /**
     * 转化路由对象  GatewayRoutes
     * @param gatewayRouteDefinitionDTO
     * @return
     */
    private GatewayRoutesPO transformToGatewayRoutes(GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO){
        GatewayRoutesPO definition = new GatewayRoutesPO();
        routeDefinitionMapper.map(gatewayRouteDefinitionDTO,definition);
        //设置路由id
        if (!StringUtils.isNotBlank(definition.getId())){
            definition.setId(java.util.UUID.randomUUID().toString().toUpperCase().replace("-",""));
        }

        String filters = JSONArray.toJSONString(gatewayRouteDefinitionDTO.getFilters());
        String predicates = JSONArray.toJSONString(gatewayRouteDefinitionDTO.getPredicates());

        definition.setFilters(filters);
        definition.setPredicates(predicates);

        return definition;
    }

    /**
     * 测试方法 新建 一个路由
     */
    //@PostConstruct
//    public static void main(String[] args) {
//        RouteDefinition definition = new RouteDefinition();
//        definition.setId("scf-user-business-gray");
//        URI uri = UriComponentsBuilder.fromUriString("lb://scf-user-business-gray").build().toUri();
////         URI uri = UriComponentsBuilder.fromHttpUrl("http://baidu.com").build().toUri();
//        definition.setUri(uri);
//        definition.setOrder(11111);
//        //定义第一个断言
//        PredicateDefinition predicate = new PredicateDefinition();
//        predicate.setName("Path");
//        Map<String, String> predicateParams = new HashMap<>(8);
//        predicateParams.put("pattern", "/scf/user-api/**");
//        predicate.setArgs(predicateParams);
//        //定义Filter
//        FilterDefinition filterDefinition = new FilterDefinition();
//        filterDefinition.setName("StripPrefix");
//        filterDefinition.addArg("parts","2");
////        Map<String, String> filterParams = new HashMap<>(8);
////        //该_genkey_前缀是固定的，见org.springframework.cloud.gateway.support.NameUtils类
////        filterParams.put("_genkey_0", "2");
//        //filterDefinition.setArgs(filterParams);
//
//        FilterDefinition filterDefinition2 = new FilterDefinition();
//        filterDefinition2.setName("RewritePath");
//        filterDefinition2.addArg("parts","2");
//
//        //过滤header
//
//
//        FilterDefinition filterDefinition1 = new FilterDefinition();
//        filterDefinition1.setName("AddRequestParameter");
//        Map<String, String> filter1Params = new HashMap<>(8);
//        filter1Params.put("_genkey_0", "param");
//        filter1Params.put("_genkey_1", "addParam");
//        filterDefinition1.setArgs(filter1Params);
//
//
//        definition.setFilters(Arrays.asList(filterDefinition,filterDefinition1));
//        definition.setPredicates(Arrays.asList(predicate));
//
//        System.out.println("definition:" + JSON.toJSONString(definition));
//        //redisTemplate.opsForHash().put(GATEWAY_ROUTES_PREFIX, "key", JSON.toJSONString(definition));
//    }
}
