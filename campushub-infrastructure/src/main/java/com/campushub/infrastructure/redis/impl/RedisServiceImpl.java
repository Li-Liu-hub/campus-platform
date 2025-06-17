package com.campushub.infrastructure.redis.impl;

import com.campushub.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
