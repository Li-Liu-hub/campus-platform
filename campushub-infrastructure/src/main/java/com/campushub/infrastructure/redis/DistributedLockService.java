package com.campushub.infrastructure.redis;

import java.time.Duration;

/**
 * 功能：定义基于 Redis 内置原子命令的分布式锁能力，不依赖 Lua 脚本。
 *
 * <p>锁的持有者以唯一 token 标识，token 同时作为锁 Hash 的字段名：获取锁即原子写入
 * 带 TTL 的自身字段，释放锁即原子读取并删除自身字段，天然不会误删他人的锁；
 * 锁 TTL 到期后字段被 Redis 自动删除，实现锁超时自动释放。
 */
public interface DistributedLockService {

    /**
     * 功能：尝试获取分布式锁。
     *
     * <p>底层使用 Redis 8.0 内置命令 HSETEX（FNX + EX）：字段不存在时原子写入并设置
     * 字段级 TTL，任何时刻只有一个请求能写入成功。
     *
     * @param key 锁键，同一把锁的请求必须使用相同键
     * @param token 锁持有者唯一令牌，同时作为锁 Hash 的字段名，必须全局唯一
     * @param ttl 锁自动释放时长，超时后由 Redis 自动删除字段完成解锁
     * @return true 获取成功；false 锁已被其他持有者占用
     * @throws org.springframework.dao.DataAccessException Redis 不可用等基础设施故障时抛出，
     *         由调用方决定是否降级处理
     */
    boolean tryLock(String key, String token, Duration ttl);

    /**
     * 功能：释放自己持有的锁。
     *
     * <p>底层使用 Redis 8.0 内置命令 HGETDEL 原子读取并删除以自身 token 为名的字段；
     * 由于字段名即持有者令牌，即使锁已超时并被他人重新获取，也不可能删除他人的锁。
     *
     * @param key 锁键，与获取锁时一致
     * @param token 锁持有者唯一令牌，与获取锁时一致
     * @return true 本次调用完成释放；false 锁已超时自动释放，说明持锁期间执行已超时
     * @throws org.springframework.dao.DataAccessException Redis 不可用等基础设施故障时抛出
     */
    boolean unlock(String key, String token);
}
