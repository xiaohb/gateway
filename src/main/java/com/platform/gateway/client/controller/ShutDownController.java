//package com.plt.scf.gateway.client.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeansException;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//
//@Slf4j
//@RestController
//public class ShutDownController implements ApplicationContextAware {
//
//    private ApplicationContext context;
//
//    @GetMapping("/shutDownContext")
//    public String shutDownContext() {
//        long  startTime = System.currentTimeMillis();
//        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) context;
//        ctx.close();
//        log.info("shutDownContext consumer time:{}",(System.currentTimeMillis()-startTime));
//        return "context is shutdown";
//    }
//
//    @GetMapping("/")
//    public String getIndex() {
//        return "OK";
//    }
//
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        context = applicationContext;
//    }
//}
