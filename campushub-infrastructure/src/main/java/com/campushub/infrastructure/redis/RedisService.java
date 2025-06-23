package com.campushub.infrastructure.redis;

import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 通用操作能力接口，业务模块统一通过本接口访问 Redis，不直接依赖 RedisTemplate。
 *
 * <p>键与值统一按字符串读写；连接配置复用 spring.data.redis 既有配置，无需额外配置。
 */
public interface RedisService {

    /** 写入字符串值，不设置过期时间。 */
    void set(String key, String value);

    /** 写入字符串值并设置过期时间，到期后自动删除。 */
    void set(String key, String value, Duration ttl);

    /** 读取字符串值，键不存在时返回 null。 */
    String get(String key);

    /** 删除键，键不存在时返回 false。 */
    Boolean delete(String key);

    /** 判断键是否存在。 */
    Boolean hasKey(String key);

    /** 为已有键设置过期时间。 */
    Boolean expire(String key, Duration ttl);

    /** 数值自增并返回自增后的结果，键不存在时从 0 开始。 */
    Long increment(String key, long delta);

    /** 数值自减并返回自减后的结果，键不存在时从 0 开始。 */
    Long decrement(String key, long delta);

    /** 向哈希指定字段累加数值增量并返回累加结果，字段不存在时从 0 开始。 */
    Long hIncrBy(String key, String field, long delta);

    /** 向有序集合写入成员及分数，成员已存在时更新分数。 */
    Boolean zAdd(String key, String member, double score);

    /** 有序集合成员分数自增并返回自增后的分数，成员不存在时视为新增。 */
    Double zIncrementScore(String key, String member, double delta);

    /** 读取有序集合成员分数，成员不存在时返回 null。 */
    Double zScore(String key, String member);

    /** 读取成员在升序中的名次，从 0 开始，成员不存在时返回 null。 */
    Long zRank(String key, String member);

    /** 读取成员在降序中的名次，从 0 开始，成员不存在时返回 null。 */
    Long zReverseRank(String key, String member);

    /** 按分数降序读取排名区间内的成员，start 与 end 为名次下标，-1 表示末尾。 */
    Set<String> zReverseRange(String key, long start, long end);

    /** 按分数降序读取排名区间内的成员及分数，用于排行榜展示。 */
    Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end);

    /** 从有序集合移除成员，返回实际移除数量。 */
    Long zRemove(String key, String member);

    /** 向集合添加成员，返回本次实际新增数量，成员已存在时返回 0。 */
    Long sAdd(String key, String member);

    /** 从集合移除成员，返回实际移除数量。 */
    Long sRemove(String key, String member);

    /** 判断成员是否在集合中。 */
    Boolean sIsMember(String key, String member);

    /** 返回集合成员数量。 */
    Long sSize(String key);

    /** 返回集合全部成员，数据量大时慎用。 */
    Set<String> sMembers(String key);
}
