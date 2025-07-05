package com.campushub.post.constant;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 帖子热度榜常量：榜单键与加权增量权重的唯一定义处。
 *
 * <p>榜单是业务行为的下游衍生品，业务接口不感知其存在，由 flush 任务与
 * MQ 消费者在落库成功后统一加分，加法模型只加增量、永不重算总分。
 */
public final class RankConstants {

    /** 榜单键前缀，日榜完整键为 campushub:rank:{yyyyMMdd}，按天分键。 */
    public static final String RANK_KEY_PREFIX = "campushub:rank:";

    /** 周榜键，由最近 7 个日榜键 ZUNIONSTORE 合成。 */
    public static final String RANK_WEEK_KEY = "campushub:rank:week";

    /** 日榜键 TTL：榜单是可再生展示数据，丢失后随新交互自然重建，敢于设短。 */
    public static final Duration RANK_TTL = Duration.ofDays(7);

    /** 热度权重：浏览一次加 1 分。 */
    public static final double WEIGHT_VIEW = 1.0;

    /** 热度权重：点赞一次加 3 分。 */
    public static final double WEIGHT_LIKE = 3.0;

    /** 热度权重：收藏一次加 5 分。 */
    public static final double WEIGHT_FAVORITE = 5.0;

    /** 生成执行时刻的日榜键，跨零点漂移仅差几条交互，接受不做事件时间路由。 */
    public static String todayKey() {
        return RANK_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /** 常量类，禁止实例化。 */
    private RankConstants() {
    }
}
