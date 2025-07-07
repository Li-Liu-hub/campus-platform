package com.campushub.infrastructure.redis.impl;

import com.campushub.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 基于 StringRedisTemplate 的 Redis 通用操作实现。 */
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 写入字符串值，不设置过期时间。 */
    @Override
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /** 写入字符串值并设置过期时间。 */
    @Override
    public void set(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    /** 读取字符串值。 */
    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /** 批量读取多个字符串键，一次网络往返取回全部值。 */
    @Override
    public List<String> multiGet(List<String> keys) {
        return stringRedisTemplate.opsForValue().multiGet(keys);
    }

    /** HGETDEL 等价 Lua 脚本：同脚本内读取后删除，整体原子执行，字段不存在返回 nil。 */
    private static final DefaultRedisScript<String> HGETDEL_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('HGET', KEYS[1], ARGV[1]) "
                    + "if v then redis.call('HDEL', KEYS[1], ARGV[1]) end "
                    + "return v",
            String.class);

    /** 原子读取并删除哈希指定字段。 */
    @Override
    public String hGetDel(String key, String field) {
        return stringRedisTemplate.execute(HGETDEL_SCRIPT, List.of(key), field);
    }

    /** 扫描哈希全部字段名，游标遍历到完成，扫描期间被删除的字段不会重复出现。 */
    @Override
    public Set<String> hScanFields(String key, long count) {
        Set<String> fields = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().count(count).build();
        try (Cursor<Map.Entry<Object, Object>> cursor = stringRedisTemplate.opsForHash().scan(key, options)) {
            while (cursor.hasNext()) {
                fields.add(String.valueOf(cursor.next().getKey()));
            }
        }
        return fields;
    }

    /** 删除键。 */
    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /** 判断键是否存在。 */
    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /** 为已有键设置过期时间。 */
    @Override
    public Boolean expire(String key, Duration ttl) {
        return stringRedisTemplate.expire(key, ttl);
    }

    /** 数值自增。 */
    @Override
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /** 数值自减。 */
    @Override
    public Long decrement(String key, long delta) {
        return stringRedisTemplate.opsForValue().decrement(key, delta);
    }

    /** 向哈希指定字段累加数值增量。 */
    @Override
    public Long hIncrBy(String key, String field, long delta) {
        return stringRedisTemplate.opsForHash().increment(key, field, delta);
    }

    /** 向有序集合写入成员及分数。 */
    @Override
    public Boolean zAdd(String key, String member, double score) {
        return stringRedisTemplate.opsForZSet().add(key, member, score);
    }

    /** 有序集合成员分数自增。 */
    @Override
    public Double zIncrementScore(String key, String member, double delta) {
        return stringRedisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    /** 读取有序集合成员分数。 */
    @Override
    public Double zScore(String key, String member) {
        return stringRedisTemplate.opsForZSet().score(key, member);
    }

    /** 读取成员在升序中的名次。 */
    @Override
    public Long zRank(String key, String member) {
        return stringRedisTemplate.opsForZSet().rank(key, member);
    }

    /** 读取成员在降序中的名次。 */
    @Override
    public Long zReverseRank(String key, String member) {
        return stringRedisTemplate.opsForZSet().reverseRank(key, member);
    }

    /** 按分数降序读取排名区间内的成员。 */
    @Override
    public Set<String> zReverseRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /** 按分数降序读取排名区间内的成员及分数。 */
    @Override
    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /** 从有序集合移除成员。 */
    @Override
    public Long zRemove(String key, String member) {
        return stringRedisTemplate.opsForZSet().remove(key, member);
    }

    /** 计算多个有序集合的并集存入目标键，权重 1、分数求和。 */
    @Override
    public Long zUnionStore(String destKey, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return stringRedisTemplate.opsForZSet()
                .unionAndStore(keys.get(0), keys.subList(1, keys.size()), destKey);
    }

    /** 向集合添加成员。 */
    @Override
    public Long sAdd(String key, String member) {
        return stringRedisTemplate.opsForSet().add(key, member);
    }

    /** 从集合移除成员。 */
    @Override
    public Long sRemove(String key, String member) {
        return stringRedisTemplate.opsForSet().remove(key, member);
    }

    /** 判断成员是否在集合中。 */
    @Override
    public Boolean sIsMember(String key, String member) {
        return stringRedisTemplate.opsForSet().isMember(key, member);
    }

    /** 返回集合成员数量。 */
    @Override
    public Long sSize(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    /** 返回集合全部成员。 */
    @Override
    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }
}
