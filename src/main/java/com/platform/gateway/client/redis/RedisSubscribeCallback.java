package com.platform.gateway.client.redis;

 
public interface RedisSubscribeCallback {
    void callback(String msg);
}
