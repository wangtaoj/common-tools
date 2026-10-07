package com.wangtao.tools.lock;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁辅助工具类
 * 提供加锁执行任务、自动释放锁的模板方法
 *
 * @author wangtao
 * Created at 2026-08-20
 */
@Slf4j
public class DistributedLockHelper {

    /**
     * 加锁成功则执行Runnable，自动释放锁（无返回值）
     *
     * @param key      锁的key
     * @param timeout  锁的过期时间
     * @param timeUnit 时间单位
     * @param runnable 加锁成功后执行的任务
     * @return 加锁成功返回true, 否则返回false
     */
    public static boolean tryRun(String key, long timeout, TimeUnit timeUnit, Runnable runnable) {
        StringRedisTemplate redisTemplate = SpringContext.getBean(StringRedisTemplate.class);
        DistributedLock distributedLock = new RedisDistributedLock(redisTemplate);
        boolean isLock = distributedLock.tryLock(key, timeout, timeUnit);
        if (isLock) {
            try {
                runnable.run();
            } finally {
                boolean isUnlock = distributedLock.unlock(key);
                if (!isUnlock) {
                    log.warn("释放锁失败，锁的key: {}", key);
                }
            }
        }
        return isLock;
    }

    /**
     * 加锁成功则执行Callable，自动释放锁，并返回执行结果
     * <p>
     *
     * @param key      锁的key
     * @param timeout  锁的过期时间
     * @param timeUnit 时间单位
     * @param callable 加锁成功后执行的任务
     * @param <T>      返回结果类型
     * @return LockResult对象
     * @throws ToolRuntimeException 任务执行中发生非运行时异常，包装后抛出
     */
    public static <T> LockResult<T> tryRunWithResult(String key, long timeout, TimeUnit timeUnit, Callable<T> callable) {
        StringRedisTemplate redisTemplate = SpringContext.getBean(StringRedisTemplate.class);
        DistributedLock distributedLock = new RedisDistributedLock(redisTemplate);
        boolean isLock = distributedLock.tryLock(key, timeout, timeUnit);
        if (!isLock) {
            return LockResult.lockNotAcquired();
        }
        T result;
        try {
            result = callable.call();
        } catch (Exception e) {
            // 将检查异常转换为运行时异常
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ToolRuntimeException(e);
        } finally {
            boolean isUnlock = distributedLock.unlock(key);
            if (!isUnlock) {
                log.warn("释放锁失败，锁的key: {}", key);
            }
        }
        return LockResult.acquired(result);
    }
}
