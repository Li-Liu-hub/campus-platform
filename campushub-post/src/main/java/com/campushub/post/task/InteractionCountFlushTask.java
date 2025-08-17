package com.campushub.post.task;

import com.campushub.infrastructure.redis.RedisService;
import com.campushub.post.constant.PostRedisKeys;
import com.campushub.post.constant.RankConstants;
import com.campushub.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * 功能：点赞/收藏计数增量定时刷库任务，把 like/fav delta 聚合 Hash 中的净增量
 * 批量折叠到 ch_post 聚合计数，并在落库成功后给当日热度榜加权加分。
 *
 * <p>与浏览增量刷库同构（时间驱动批量聚合）：HSCAN 枚举字段、HGETDEL 原子取走增量、
 * 批量落库，DB 失败补偿记回 Hash 下轮重试；榜单加分在 DB 成功之后且失败不补偿
 * （展示型数据 at-most-once）。点赞与收藏两个维度独立刷库互不阻塞，
 * 榜单增量按各自权重独立累加，不要求与其他维度同步刷库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionCountFlushTask {

    private final RedisService redisService;

    private final PostMapper postMapper;

    /** HSCAN 每批扫描的字段数。 */
    private static final long SCAN_BATCH_SIZE = 500;

    /**
     * 功能：按固定间隔刷库，间隔可经 campushub.post.interaction-flush-interval-ms 配置，默认 5 分钟。
     *
     * <p>点赞、收藏两个维度各自独立容错：任一维度整体异常不影响另一维度本轮执行；
     * 维度内单条增量失败补偿后继续处理其余字段。
     */
    @Scheduled(fixedDelayString = "${campushub.post.interaction-flush-interval-ms:300000}")
    public void flush() {
        int likeFlushed = flushSafely(
                PostRedisKeys.LIKE_DELTA_KEY, RankConstants.WEIGHT_LIKE, this::persistLikeDelta);
        int favFlushed = flushSafely(
                PostRedisKeys.FAV_DELTA_KEY, RankConstants.WEIGHT_FAVORITE, this::persistFavDelta);
        log.info("互动计数增量刷库完成，点赞成功落库 {} 个，收藏成功落库 {} 个", likeFlushed, favFlushed);
    }

    /** 维度级容错包装：整体异常记日志返回 0，增量保留在 Hash 中等待下轮。 */
    private int flushSafely(String deltaKey, double weight, BiPredicate<String, Long> persist) {
        try {
            return flushDimension(deltaKey, weight, persist);
        } catch (Exception exception) {
            log.error("互动计数刷库异常，本维度本轮终止，增量保留待下轮，deltaKey={}", deltaKey, exception);
            return 0;
        }
    }

    /**
     * 功能：刷库单个维度的计数增量。
     *
     * @param deltaKey 聚合计数 Hash 键
     * @param weight 该维度的热度榜权重
     * @param persist 落库函数，入参 (帖子 ID 字符串, 净增量)，返回 true 表示成功或已按丢弃处理
     * @return 本轮成功落库的字段数
     */
    private int flushDimension(String deltaKey, double weight, BiPredicate<String, Long> persist) {
        Set<String> fields = redisService.hScanFields(deltaKey, SCAN_BATCH_SIZE);
        if (fields.isEmpty()) {
            return 0;
        }
        String rankKey = RankConstants.todayKey();
        int flushed = 0;
        for (String field : fields) {
            // HGETDEL 原子取走增量：并发互动补进的新值不会被误取，落库失败时原样记回
            Long delta = takeDelta(deltaKey, field);
            if (delta == null || delta == 0) {
                continue;
            }
            if (persist.test(field, delta)) {
                addRankScore(rankKey, field, weight * delta);
                flushed++;
            }
        }
        log.info("互动计数维度刷库完成，deltaKey={}，本批扫描 {} 个字段，成功落库 {} 个",
                deltaKey, fields.size(), flushed);
        return flushed;
    }

    /** 点赞计数增量落库，失败时增量原样记回 Hash 等待下轮重试。 */
    private boolean persistLikeDelta(String field, Long delta) {
        return persistDelta(PostRedisKeys.LIKE_DELTA_KEY, field, delta, postMapper::adjustLikeNumber);
    }

    /** 收藏计数增量落库，失败时增量原样记回 Hash 等待下轮重试。 */
    private boolean persistFavDelta(String field, Long delta) {
        return persistDelta(PostRedisKeys.FAV_DELTA_KEY, field, delta, postMapper::adjustCollectNumber);
    }

    /**
     * 功能：将净增量折叠到 ch_post 聚合计数，失败时增量原样记回 Hash 等待下轮重试。
     *
     * @param deltaKey 聚合计数 Hash 键，补偿写回用
     * @param field 帖子 ID 字符串
     * @param delta 净增量，正负均可
     * @param updater 聚合计数 UPDATE 函数，入参 (postId, delta)
     * @return true 表示落库成功（或帖子已不存在按丢弃处理），false 表示本条失败已补偿
     */
    private boolean persistDelta(String deltaKey, String field, long delta,
            BiFunction<Long, Integer, Integer> updater) {
        try {
            int rows = updater.apply(Long.valueOf(field), (int) delta);
            if (rows == 0) {
                // 帖子已物理不存在，增量无归属，直接丢弃
                log.warn("帖子不存在，互动计数增量丢弃，deltaKey={}，postId={}", deltaKey, field);
            }
            return true;
        } catch (Exception exception) {
            log.error("互动计数落库失败，补偿记回 Hash，deltaKey={}，postId={}，delta={}",
                    deltaKey, field, delta, exception);
            compensate(deltaKey, field, delta);
            return false;
        }
    }

    /** 原子取走指定帖子的互动计数增量，字段不存在返回 null，Redis 异常时返回 null 留待下轮。 */
    private Long takeDelta(String deltaKey, String field) {
        try {
            String value = redisService.hGetDel(deltaKey, field);
            return value == null ? null : Long.valueOf(value);
        } catch (Exception exception) {
            log.warn("互动计数读取失败，本条跳过，deltaKey={}，postId={}", deltaKey, field, exception);
            return null;
        }
    }

    /** 落库失败补偿：增量记回 Hash 供下轮重试，补偿也失败则记 error 供人工对账。 */
    private void compensate(String deltaKey, String field, long delta) {
        try {
            redisService.hIncrBy(deltaKey, field, delta);
        } catch (Exception compensateException) {
            log.error("互动计数补偿失败，存在丢失风险，deltaKey={}，postId={}，delta={}",
                    deltaKey, field, delta, compensateException);
        }
    }

    /** 给当日榜加权加分（维度权重 × 净增量），失败仅记日志；每次加分后续期日榜 TTL。 */
    private void addRankScore(String rankKey, String postIdField, double delta) {
        try {
            redisService.zIncrementScore(rankKey, postIdField, delta);
            redisService.expire(rankKey, RankConstants.RANK_TTL);
        } catch (Exception exception) {
            log.error("榜单加分失败，容忍少量偏差，postId={}，delta={}", postIdField, delta, exception);
        }
    }
}
