package com.wangtao.tools.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口
 *
 * @author wangtao
 * Created at 2026-08-20
 */
public interface DistributedLock {

    /**
     * 尝试加锁
     *
     * @param key      锁的key
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return true-加锁成功，false-加锁失败
     */
    boolean tryLock(String key, long timeout, TimeUnit timeUnit);

    /**
     * 释放锁
     *
     * @param key 锁的key
     * @return true-释放成功，false-释放失败
     */
    boolean unlock(String key);
}
