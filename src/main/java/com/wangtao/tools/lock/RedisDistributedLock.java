package com.wangtao.tools.lock;

import com.wangtao.tools.util.UuidUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁实现
 *
 * @author wangtao
 * Created at 2026-08-20
 */
@Slf4j
public class RedisDistributedLock implements DistributedLock {

    private static final String KEY_PREFIX = "lock:";

    private static final String LUA_SCRIPT_STR = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT_STR, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁持有者标识
     */
    private final String owner;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.owner = UuidUtils.uuid();
    }

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit timeUnit) {
        String realKey = KEY_PREFIX + key;
        Boolean res = redisTemplate.opsForValue().setIfAbsent(realKey, owner, timeout, timeUnit);
        return res != null && res;
    }

    @Override
    public boolean unlock(String key) {
        String realKey = KEY_PREFIX + key;
        Long res = redisTemplate.execute(UNLOCK_LUA_SCRIPT, Collections.singletonList(realKey), owner);
        return res != null && res == 1L;
    }
}
