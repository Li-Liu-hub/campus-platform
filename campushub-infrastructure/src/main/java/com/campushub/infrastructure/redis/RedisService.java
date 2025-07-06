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

    /** 批量读取多个字符串键，返回与 keys 顺序一致的值列表，不存在的键对应位置为 null。 */
    java.util.List<String> multiGet(java.util.List<String> keys);

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

    /**
     * 原子读取并删除哈希指定字段（HGETDEL 语义），适用于聚合增量刷库场景。
     *
     * <p>服务端 Redis 7.4 起提供原生 HGETDEL，客户端 API 未暴露前以等价 Lua
     * （HGET + HDEL，同脚本原子执行）实现，读取与删除之间不会插入其他命令。
     */
    String hGetDel(String key, String field);

    /** 扫描哈希全部字段名并完整返回，内部按游标分批遍历，超大批量哈希慎用。 */
    java.util.Set<String> hScanFields(String key, long count);

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

    /**
     * 计算多个有序集合的并集并存入目标键，返回目标键的成员数量。
     *
     * <p>所有源键权重为 1，同成员分数聚合方式为求和，用于周榜合成等场景；
     * 源键全部不存在时目标键会被写成空集合。
     */
    Long zUnionStore(String destKey, java.util.List<String> keys);

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
