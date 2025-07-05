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

/**
 * 功能：浏览增量定时刷库任务，把 campushub:post:view:delta 聚合 Hash 中的增量批量落库，
 * 并在落库成功后给当日热度榜加权加分。
 *
 * <p>浏览是高频可折叠事件，与点赞/收藏的单行事件不同，采用时间驱动批量聚合：
 * 定时任务驱动，HSCAN 枚举字段、HGETDEL 原子取走增量、批量落库，DB 失败补偿记回 Hash 下轮重试，
 * 榜单加分在 DB 成功之后且失败不补偿（展示型数据 at-most-once）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushTask {

    private final RedisService redisService;

    private final PostMapper postMapper;

    /** HSCAN 每批扫描的字段数。 */
    private static final long SCAN_BATCH_SIZE = 500;

    /**
     * 功能：按固定间隔刷库，间隔可经 campushub.post.view-flush-interval-ms 配置，默认 5 分钟。
     *
     * <p>单条增量处理互不影响：某条失败补偿后继续处理其余字段。整体异常吞掉记日志，
     * 保证定时任务不因单次故障停摆。
     */
    @Scheduled(fixedDelayString = "${campushub.post.view-flush-interval-ms:300000}")
    public void flush() {
        try {
            Set<String> fields = redisService.hScanFields(PostRedisKeys.VIEW_DELTA_KEY, SCAN_BATCH_SIZE);
            if (fields.isEmpty()) {
                return;
            }
            String rankKey = RankConstants.todayKey();
            int flushed = 0;
            for (String field : fields) {
                // HGETDEL 原子取走增量：并发浏览补进的新值不会被误取，落库失败时原样记回
                Long delta = takeDelta(field);
                if (delta == null || delta == 0) {
                    continue;
                }
                if (!persist(field, delta)) {
                    continue;
                }
                addRankScore(rankKey, field, delta);
                flushed++;
            }
            log.info("浏览增量刷库完成，本批扫描 {} 个字段，成功落库 {} 个", fields.size(), flushed);
        } catch (Exception exception) {
            log.error("浏览增量刷库任务异常，本轮终止，增量保留待下轮", exception);
        }
    }

    /** 原子取走指定帖子的浏览增量，字段不存在返回 null，Redis 异常时返回 null 留待下轮。 */
    private Long takeDelta(String field) {
        try {
            String value = redisService.hGetDel(PostRedisKeys.VIEW_DELTA_KEY, field);
            return value == null ? null : Long.valueOf(value);
        } catch (Exception exception) {
            log.warn("浏览增量读取失败，本条跳过，postId={}", field, exception);
            return null;
        }
    }

    /**
     * 功能：将浏览增量落库到帖子表，失败时增量原样记回 Hash 等待下轮重试。
     *
     * @param field 帖子 ID 字符串
     * @param delta 浏览增量
     * @return true 表示落库成功（或帖子已不存在按丢弃处理），false 表示本条失败已补偿
     */
    private boolean persist(String field, long delta) {
        try {
            int rows = postMapper.increaseViewNumberBy(Long.valueOf(field), delta);
            if (rows == 0) {
                // 帖子已物理不存在，增量无归属，直接丢弃
                log.warn("帖子不存在，浏览增量丢弃，postId={}", field);
            }
            return true;
        } catch (Exception exception) {
            log.error("浏览增量落库失败，补偿记回 Hash，postId={}，delta={}", field, delta, exception);
            try {
                redisService.hIncrBy(PostRedisKeys.VIEW_DELTA_KEY, field, delta);
            } catch (Exception compensateException) {
                // 补偿也失败意味着本批增量可能丢失，记 error 供人工对账
                log.error("浏览增量补偿失败，存在丢失风险，postId={}，delta={}", field, delta, compensateException);
            }
            return false;
        }
    }

    /** 给当日榜加权加分（浏览权重 × 增量），失败仅记日志；每次加分后续期日榜 TTL。 */
    private void addRankScore(String rankKey, String postIdField, long delta) {
        try {
            redisService.zIncrementScore(rankKey, postIdField, RankConstants.WEIGHT_VIEW * delta);
            redisService.expire(rankKey, RankConstants.RANK_TTL);
        } catch (Exception exception) {
            log.error("榜单加分失败，容忍少量偏差，postId={}，delta={}", postIdField, delta, exception);
        }
    }
}
