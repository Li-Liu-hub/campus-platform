package com.campushub.post.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.post.constant.PostRedisKeys;
import com.campushub.post.constant.RankConstants;
import com.campushub.post.service.PostRankService;
import com.campushub.post.service.PostService;
import com.campushub.post.vo.PostVO;
import com.campushub.post.vo.RankItemVO;
import com.campushub.post.vo.RankVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 功能：热度榜查询实现，读路径为 ZREVRANGE + 批量详情组装，MySQL 压力趋近于零。
 *
 * <p>榜单条目的帖子概要优先从详情缓存批量取（multiGet 一次网络往返），
 * 未命中的条目回落到既有 Cache-Aside 单键回源（含写回），已删除帖子跳过不占榜位。
 * 周榜用临时键 + 短 TTL 合成：首个请求触发 ZUNIONSTORE，窗口内后续请求直读，
 * 本质是一次迷你 Cache-Aside。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostRankServiceImpl implements PostRankService {

    private final RedisService redisService;

    private final PostService postService;

    private final ObjectMapper objectMapper;

    /** 榜单默认条数。 */
    private static final int DEFAULT_TOP_N = 10;

    /** 榜单最大条数。 */
    private static final int MAX_TOP_N = 100;

    /** 周榜合成结果缓存时长，窗口内的看榜请求不再做 O(N) 聚合。 */
    private static final Duration WEEK_CACHE_TTL = Duration.ofMinutes(10);

    /** 周榜聚合的日榜数量。 */
    private static final int WEEK_DAY_COUNT = 7;

    /** 查询当日热度榜前 N 名。 */
    @Override
    public List<RankItemVO> top(Integer n) {
        return readBoard(RankConstants.todayKey(), resolveN(n));
    }

    /** 查询近 7 日周榜前 N 名，先确保周榜临时键存在。 */
    @Override
    public List<RankItemVO> week(Integer n) {
        ensureWeekBoard();
        return readBoard(RankConstants.RANK_WEEK_KEY, resolveN(n));
    }

    /** 查询指定帖子当日名次与热度分，ZREVRANK 返回 0 基名次，对外统一为 1 基。 */
    @Override
    public RankVO rankOf(Long postId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子 ID 不能为空");
        }
        String rankKey = RankConstants.todayKey();
        Long rank = redisService.zReverseRank(rankKey, String.valueOf(postId));
        Double score = redisService.zScore(rankKey, String.valueOf(postId));
        if (rank == null || score == null) {
            return new RankVO(postId, null, null);
        }
        return new RankVO(postId, rank + 1, score);
    }

    /**
     * 功能：读取榜单区间并组装条目。
     *
     * <p>先 ZREVRANGE WITHSCORES 拿 postId 与分数，再 multiGet 批量读详情缓存；
     * 缓存未命中或内容损坏的条目回落 getById 走 Cache-Aside 回源，
     * 帖子已删除（404）时跳过，后续名次自然前移。
     *
     * @param key 榜单键（日榜或周榜）
     * @param n 条数上限
     * @return 榜单条目列表，榜为空时返回空列表
     */
    private List<RankItemVO> readBoard(String key, int n) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisService.zReverseRangeWithScores(key, 0, n - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            postIds.add(Long.valueOf(tuple.getValue()));
        }
        // 一次网络往返批量取详情缓存，命中时 MySQL 全程无感知
        List<String> cached = redisService.multiGet(postIds.stream()
                .map(postId -> PostRedisKeys.INFO_KEY_PREFIX + postId)
                .toList());
        List<RankItemVO> result = new ArrayList<>(postIds.size());
        int index = 0;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long postId = postIds.get(index);
            double score = tuple.getScore() == null ? 0 : tuple.getScore();
            PostVO postVO = parseCachedPost(cached.get(index), postId);
            if (postVO == null) {
                try {
                    // 缓存未命中走既有 Cache-Aside 回源（含写回），帖子已删除则抛 404
                    postVO = postService.getById(postId);
                } catch (BusinessException exception) {
                    index++;
                    continue;
                }
            }
            result.add(new RankItemVO(postId, postVO.postTitle(), postVO.postType(),
                    postVO.postViewNumber(), postVO.createTime(), score, result.size() + 1L));
            index++;
        }
        return result;
    }

    /** 解析缓存中的帖子详情 JSON，未命中或反序列化失败返回 null 交由回源处理。 */
    private PostVO parseCachedPost(String json, Long postId) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PostVO.class);
        } catch (Exception exception) {
            log.warn("榜单组装时详情缓存反序列化失败，回落回源，postId={}", postId, exception);
            return null;
        }
    }

    /**
     * 功能：确保周榜临时键存在，不存在则合成最近 7 个日榜。
     *
     * <p>并发首个请求同时触发合成是幂等的，代价只是多一次 O(N) 聚合。
     * 七个日榜全空时 ZUNIONSTORE 会写出一个空结果键，主动删除避免空榜被缓存 10 分钟。
     */
    private void ensureWeekBoard() {
        if (Boolean.TRUE.equals(redisService.hasKey(RankConstants.RANK_WEEK_KEY))) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<String> dayKeys = new ArrayList<>(WEEK_DAY_COUNT);
        for (int i = 0; i < WEEK_DAY_COUNT; i++) {
            dayKeys.add(RankConstants.RANK_KEY_PREFIX
                    + today.minusDays(i).format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        Long size = redisService.zUnionStore(RankConstants.RANK_WEEK_KEY, dayKeys);
        if (size == null || size == 0) {
            redisService.delete(RankConstants.RANK_WEEK_KEY);
            return;
        }
        redisService.expire(RankConstants.RANK_WEEK_KEY, WEEK_CACHE_TTL);
    }

    /** 条数缺省 10，并限制在 1 到 100 之间。 */
    private int resolveN(Integer n) {
        if (n == null) {
            return DEFAULT_TOP_N;
        }
        return Math.min(Math.max(n, 1), MAX_TOP_N);
    }
}
