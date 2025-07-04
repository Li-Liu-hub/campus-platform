package com.campushub.infrastructure.redis.impl;

import com.campushub.infrastructure.redis.DistributedLockService;
import io.lettuce.core.HSetExArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 功能：基于 Redis 8.0 内置原子命令 HSETEX 与 HGETDEL 的分布式锁实现（Lettuce 6.6 起
 * 提供这两条命令的类型安全 API），全程不使用 Lua 脚本。
 *
 * <p>锁结构为 Hash：字段名 = 持有者 token，字段值固定为 "1"。
 * 获取锁 = HSETEX FNX EX（字段不存在才写入并带字段级 TTL）；
 * 释放锁 = HGETDEL 自身字段（原子的"读 + 删"，且不可能命中他人字段）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 自定义命令同步等待上限，超出视为 Redis 故障。 */
    private static final long COMMAND_TIMEOUT_SECONDS = 5;

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        Boolean acquired = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            RedisAsyncCommands<byte[], byte[]> commands = nativeAsyncCommands(connection);
            // FNX：字段不存在才写入；EX：字段级 TTL，到期自动删除实现锁超时自动释放
            Map<byte[], byte[]> field = Map.of(
                    token.getBytes(StandardCharsets.UTF_8),
                    "1".getBytes(StandardCharsets.UTF_8));
            Long result = await(commands.hsetex(
                    key.getBytes(StandardCharsets.UTF_8), new HSetExArgs().fnx().ex(ttl), field));
            return result != null && result == 1L;
        });
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public boolean unlock(String key, String token) {
        Boolean released = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            RedisAsyncCommands<byte[], byte[]> commands = nativeAsyncCommands(connection);
            // 字段名即自身 token：原子的"读 + 删"，锁已超时或被他人持有时仅返回空列表，不会误删
            List<KeyValue<byte[], byte[]>> values = await(commands.hgetdel(
                    key.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)));
            return values != null && !values.isEmpty() && values.get(0).hasValue();
        });
        return Boolean.TRUE.equals(released);
    }

    /** 从 Spring 连接中取出 Lettuce 原生异步命令接口，非 Lettuce 驱动时快速失败。 */
    @SuppressWarnings("unchecked")
    private RedisAsyncCommands<byte[], byte[]> nativeAsyncCommands(
            org.springframework.data.redis.connection.RedisConnection connection) {
        if (connection.getNativeConnection() instanceof RedisAsyncCommands<?, ?> rawCommands) {
            return (RedisAsyncCommands<byte[], byte[]>) rawCommands;
        }
        throw new IllegalStateException("分布式锁仅支持 Lettuce 驱动");
    }

    /** 同步等待异步命令执行结果，中断与超时统一转为运行时异常。 */
    private <T> T await(RedisFuture<T> future) {
        try {
            return future.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Redis 命令执行被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Redis 命令执行失败", exception);
        }
    }
}
