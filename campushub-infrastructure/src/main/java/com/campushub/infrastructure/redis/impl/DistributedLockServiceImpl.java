package com.campushub.infrastructure.redis.impl;

import com.campushub.infrastructure.redis.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 功能：基于 Redis String 结构 + Lua 脚本的分布式锁实现。
 *
 * <p>锁结构为 String：键名 = 锁键（如订单锁 campushub:order:lock:{orderId}），
 * 键值 = 持有者 token，键级 TTL 到期自动删除实现锁超时释放。
 * 获取锁 = SET NX EX（键不存在才写入，键级原子互斥）；
 * 释放锁 = Lua 脚本原子执行"GET 比对自身 token → 匹配才 DEL"，
 * 锁已超时或被他人持有时比对失败、不会误删他人的锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 释放锁 Lua 脚本：值等于持有者 token 才删除，比对与删除在 Redis 服务端原子完成。 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        // SET key token NX EX：NX 为键级条件（整个键不存在才写入），互斥语义严格
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl));
    }

    @Override
    public boolean unlock(String key, String token) {
        Long released = stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
        return released != null && released == 1L;
    }
}
