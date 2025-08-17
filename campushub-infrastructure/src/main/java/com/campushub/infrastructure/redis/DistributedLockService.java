package com.campushub.infrastructure.redis;

import java.time.Duration;

/**
 * 功能：定义基于 Redis String 结构的分布式锁能力，释放锁依赖 Lua 脚本保证原子性。
 *
 * <p>锁的持有者以唯一 token 标识，token 存放在锁键的值中：获取锁即 SET NX EX 原子写入
 * 带 TTL 的键值，释放锁即 Lua 脚本原子执行"比对值等于自身 token 才删除"，不会误删
 * 他人的锁；锁 TTL 到期后键被 Redis 自动删除，实现锁超时自动释放。
 */
public interface DistributedLockService {

    /**
     * 功能：尝试获取分布式锁。
     *
     * <p>底层使用 SET key token NX EX：NX 为键级条件（整个键不存在才写入），
     * 并原子设置键级 TTL，任何时刻只有一个请求能写入成功。
     *
     * @param key 锁键，同一把锁的请求必须使用相同键
     * @param token 锁持有者唯一令牌，作为锁键的值存放，释放锁时用于比对，必须全局唯一
     * @param ttl 锁自动释放时长，超时后由 Redis 自动删除键完成解锁
     * @return true 获取成功；false 锁已被其他持有者占用
     * @throws org.springframework.dao.DataAccessException Redis 不可用等基础设施故障时抛出，
     *         由调用方决定是否降级处理
     */
    boolean tryLock(String key, String token, Duration ttl);

    /**
     * 功能：释放自己持有的锁。
     *
     * <p>底层使用 Lua 脚本原子执行"GET 比对值等于自身 token → 匹配才 DEL"；
     * 比对与删除在 Redis 服务端一次完成，即使锁已超时并被他人重新获取，也不会误删他人的锁。
     *
     * @param key 锁键，与获取锁时一致
     * @param token 锁持有者唯一令牌，与获取锁时一致
     * @return true 本次调用完成释放；false 锁值非自身 token（已超时自动释放或被他人持有）
     * @throws org.springframework.dao.DataAccessException Redis 不可用等基础设施故障时抛出
     */
    boolean unlock(String key, String token);
}
