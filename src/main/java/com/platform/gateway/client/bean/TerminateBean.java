package com.platform.gateway.client.bean;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;

@Slf4j
public class TerminateBean {
    @PreDestroy
    public void preDestroy() {
        log.info("TerminalBean is destroyed");
    }
}
