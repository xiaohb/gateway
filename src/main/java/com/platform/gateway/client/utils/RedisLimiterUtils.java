package com.platform.gateway.client.utils;



import com.platform.gateway.client.redis.RedisUtil;
import com.plt.scf.common.result.Result;
import com.plt.scf.common.result.ResultGenerator;
import com.plt.scf.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.UUID;


@Slf4j
@Component
public class RedisLimiterUtils {

//    public static final String API_WEB_TIME_KEY = "time_key:";
//    public static final String API_WEB_COUNTER_KEY = "counter_key:";
    private static final String EXCEEDS_LIMIT = "规定的时间内超出了访问的限制！";
    private static final String DAY_LIMIT = "日内超出了访问的限制！";

    @Resource(name = "stringRedisTemplate")
    ValueOperations<String, String> ops;

    //上次删除日期
    private  static  String  delDate = "";

    @Resource
    private RedisUtil redisUtil;

    public Result IpRateLimiter(String ip, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        String time_key = "time_key:ip:" + ip;
        String counter_key = "counter_key:ip:" + ip;

        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }

        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("time_key:{},{}",time_key,EXCEEDS_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,ResultCode.EXCEEDS_LIMIT.msg,null);
        }
        return ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }


    public Result clientRateLimiter(String clientid, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        String time_key = "time_key:clientid:" + clientid;
        String counter_key = "counter_key:clientid:" + clientid;
        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }
        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("clientRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,EXCEEDS_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,ResultCode.EXCEEDS_LIMIT.msg,null);
        }
        return  ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }

    public Result urlRateLimiter(String path, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        String time_key = "time_key:path:" + path;
        String counter_key = "counter_key:path:" + path;
        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }
        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("urlRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,EXCEEDS_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,ResultCode.EXCEEDS_LIMIT.msg,null);
        }
        return  ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }


    public Result clientPathRateLimiter(String clientid, String access_path, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        String time_key = "time_key:clientid:" + clientid + ":path:" + access_path;
        String counter_key = "counter_key:clientid:" + clientid + ":path:" + access_path;

        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }
        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("clientPathRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,EXCEEDS_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,ResultCode.EXCEEDS_LIMIT.msg,null);
        }
        return  ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }


    public Result rateLimitOfDay(String clientid, String access_path, long limit) {

        //删除之前的日期的缓存数据,避免缓存累计太多
        String time_key = "";
        String counter_key = "";
        /** 删除旧的限流数据  **/
//            String  oldDeleteDate = "";
//            Calendar  currentDate = Calendar.getInstance();
//            currentDate.set(Calendar.DAY_OF_MONTH,currentDate.get(Calendar.DAY_OF_MONTH)-2);
//            oldDeleteDate = DateUtils.format(currentDate.getTime(),DateUtils.DATE_PATTERN);
//            time_key = "time_key:date:" + oldDeleteDate + ":clientid:" + clientid + ":path:" + access_path;
//            counter_key = "counter_key:date:" + oldDeleteDate + ":clientid:" + clientid + ":path:" + access_path;
//            if (redisUtil.hasKey(time_key)) {
//                //删除昨天的缓存数据
//                redisUtil.del(time_key);
//                redisUtil.del(counter_key);
//                log.info(" old data in redis delete,time_key:{},counter_key:{}",time_key,counter_key);
//            } else {
//                log.info("old data key is not found , redis time_key:{}",time_key);
//            }
//            delDate = oldDeleteDate;

        LocalDate  today = LocalDate.now();
        time_key = "time_key:date:" + today + ":clientid:" + clientid + ":path:" + access_path;
        counter_key = "counter_key:date:" + today + ":clientid:" + clientid + ":path:" + access_path;
        String identifier = UUID.randomUUID().toString();
        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            //当天首次访问，初始化访问计数=0，有效期24h
            redisUtil.set(time_key, identifier, 24 * 60 * 60);
            redisUtil.set(counter_key, 0);
        }

        //累加访问次数， 超出配置的limit则返回错误
        if (redisUtil.incr(counter_key, 1) > limit) {
            log.warn("clientPathRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,DAY_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,DAY_LIMIT,null);
        }
        return ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }

    /**
     * 依据访问令牌限制该用户访问次数
     * @param clientid
     * @param accessToken
     * @param limit
     * @param timeout
     * @return
     */
    public Result accessTokenRateLimiter(String clientid, String accessToken, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        String time_key = "time_key:clientid:" + clientid + ":accessToken:" + accessToken;
        String counter_key = "counter_key:clientid:" + clientid + ":accessToken:" + accessToken;

        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }
        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("accessTokenRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,EXCEEDS_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,ResultCode.EXCEEDS_LIMIT.msg,null);
        }
        return  ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }


    public Result acquireRateLimiter(String clientid, String access_path, int limit, int timeout) {
        String identifier = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        String time_key = "time_key:date:" + today + ":clientid:" + clientid + ":path:" + access_path;
        String counter_key = "counter_key:date:" + today + ":clientid:" + clientid + ":path:" + access_path;

        if (!redisUtil.hasKey(time_key) || redisUtil.getExpire(time_key) <= 0) {
            redisUtil.set(time_key, identifier, timeout);
            redisUtil.set(counter_key, 0);
        }
        if (redisUtil.hasKey(time_key) && redisUtil.incr(counter_key, 1) > limit) {
            log.warn("acquireRateLimiter time_key:{},counter_key:{},{}",time_key,counter_key,DAY_LIMIT);
            return ResultGenerator.genResult(ResultCode.EXCEEDS_LIMIT.code,DAY_LIMIT,null);
        }
        return ResultGenerator.genSuccessResult("调用次数:" + this.ops.get(counter_key));
    }


    public void save(String tokenType, String Token, int timeout) {
        redisUtil.set(tokenType, Token, timeout);
    }


    public String getToken(String tokenType) {
        return redisUtil.get(tokenType).toString();
    }


    public void saveObject(String key, Object obj, long timeout) {
        redisUtil.set(key, obj, timeout);
    }


    public void saveObject(String key, Object obj) {
        redisUtil.set(key, obj);
    }


    public Object getObject(String key) {
        return redisUtil.get(key);
    }


    public void removeObject(String key) {
        redisUtil.del(key);
    }
}