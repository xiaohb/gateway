package com.platform.gateway.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认降级处理
 */
@Slf4j
@RestController
public class DefaultHystrixController {

    @RequestMapping("/defaultfallback")
    public Map<String,String> defaultfallback(){
        Map<String,String> map = new LinkedHashMap<>();
        map.put("code", HttpStatus.INTERNAL_SERVER_ERROR+"");
        map.put("message","服务降级");
        log.warn("defaultfallback !!!");
        return map;
    }
}