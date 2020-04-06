//package com.plt.scf.gateway.client.controller;
//
//
//import com.plt.platform.common.web.PageResult;
//import com.plt.scf.gateway.client.dto.GatewayRouteDefinitionDTO;
//import com.plt.scf.gateway.client.service.DynamicRouteService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//import com.plt.scf.common.result.Result;
//import com.plt.scf.common.result.ResultGenerator;
//
//
//@RestController
//@RequestMapping("/route")
//public class RouteController {
//
//    @Autowired
//    private DynamicRouteService dynamicRouteService;
//
//    //增加路由
//    @PostMapping("/add")
//    public Result add(@RequestBody GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO) {
//        return ResultGenerator.genSuccessResult(dynamicRouteService.add(gatewayRouteDefinitionDTO));
//    }
//
//    //更新路由
//    @PostMapping("/update")
//    public Result update(@RequestBody GatewayRouteDefinitionDTO gatewayRouteDefinitionDTO) {
//        return ResultGenerator.genSuccessResult(dynamicRouteService.update(gatewayRouteDefinitionDTO));
//    }
//
//    //删除路由
//    @DeleteMapping("/{id}")
//    public Result delete(@PathVariable String id) {
//        return ResultGenerator.genSuccessResult(dynamicRouteService.delete(id));
//    }
//
//    //获取全部数据
//    @GetMapping("/findAll")
//    public PageResult findAll(@RequestParam Map<String, Object> params){
//        return dynamicRouteService.findAll(params);
//    }
//
//    //同步redis数据 从mysql中同步过去
//    @GetMapping("/synchronization")
//    public Result synchronization() {
//        return ResultGenerator.genSuccessResult(dynamicRouteService.synchronization());
//    }
//
//
//    //修改路由状态
//    @GetMapping("/updateFlag")
//    public Result updateFlag(@RequestParam Map<String, Object> params) {
//        return ResultGenerator.genSuccessResult(dynamicRouteService.updateFlag(params));
//    }
//
//
//
//
//
//}
