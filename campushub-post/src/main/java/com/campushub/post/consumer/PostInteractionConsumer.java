package com.campushub.post.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.post.constant.RankConstants;
import com.campushub.post.entity.PostCollect;
import com.campushub.post.entity.PostLike;
import com.campushub.post.mapper.PostCollectMapper;
import com.campushub.post.mapper.PostLikeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 功能：帖子互动事件消费者，把点赞/收藏事件落库到 ch_like / ch_collect，并在落库成功后给热度榜加分。
 *
 * <p>运行在 Spring AMQP 容器线程池上，AUTO 确认模式：方法正常返回自动 ACK，
 * 抛出异常由框架本地重试，重试耗尽后拒绝且不重回队列，经死信路由进入 DLQ 人工处理。
 *
 * <p>榜单加分铁律：DB 先、榜单后。DB 失败抛异常触发重试，若榜单先加分，重试轮会重复加分；
 * 榜单加分放在 DB 成功之后且失败不补偿（展示型数据 at-most-once，最坏少记几分）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostInteractionConsumer {

    private final PostLikeMapper postLikeMapper;

    private final PostCollectMapper postCollectMapper;

    private final RedisService redisService;

    /**
     * 功能：消费帖子互动事件，按动作类型分发到对应处理逻辑。
     *
     * @param event 互动事件，容器按 JSON 反序列化
     * @throws Exception 落库失败时抛出，触发容器本地重试与死信兜底
     */
    @RabbitListener(queues = MqConstants.POST_INTERACTION_QUEUE)
    public void onInteraction(PostInteractionEvent event) {
        switch (event.action()) {
            case LIKE -> like(event);
            case UNLIKE -> unlike(event);
            case COLLECT -> collect(event);
            case UNCOLLECT -> uncollect(event);
        }
    }

    /** 插入点赞记录，唯一键冲突说明重复消息，幂等丢弃；落库成功后榜单加点赞权重分。 */
    private void like(PostInteractionEvent event) {
        PostLike postLike = new PostLike();
        postLike.setLikePostId(event.postId());
        postLike.setLikeUserId(event.userId());
        try {
            postLikeMapper.insert(postLike);
        } catch (DuplicateKeyException exception) {
            // 唯一键 (like_post_id, like_user_id) 冲突：重复投递或 Redis 判重标记丢失，以 DB 为准幂等丢弃
            log.info("点赞记录已存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        addRankScore(event.postId(), RankConstants.WEIGHT_LIKE);
    }

    /** 删除点赞记录，记录不存在说明重复消息，幂等丢弃且不扣分；删除成功后榜单扣点赞权重分。 */
    private void unlike(PostInteractionEvent event) {
        int rows = postLikeMapper.delete(Wrappers.<PostLike>lambdaQuery()
                .eq(PostLike::getLikePostId, event.postId())
                .eq(PostLike::getLikeUserId, event.userId()));
        if (rows == 0) {
            // 记录不存在：重复的取消消息，不执行榜单扣分，避免重复扣分清空分数
            log.info("点赞记录不存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        addRankScore(event.postId(), -RankConstants.WEIGHT_LIKE);
    }

    /** 插入收藏记录，幂等语义与点赞一致；落库成功后榜单加收藏权重分。 */
    private void collect(PostInteractionEvent event) {
        PostCollect postCollect = new PostCollect();
        postCollect.setCollectPostId(event.postId());
        postCollect.setCollectUserId(event.userId());
        try {
            postCollectMapper.insert(postCollect);
        } catch (DuplicateKeyException exception) {
            log.info("收藏记录已存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        addRankScore(event.postId(), RankConstants.WEIGHT_FAVORITE);
    }

    /** 删除收藏记录，幂等语义与取消点赞一致；删除成功后榜单扣收藏权重分。 */
    private void uncollect(PostInteractionEvent event) {
        int rows = postCollectMapper.delete(Wrappers.<PostCollect>lambdaQuery()
                .eq(PostCollect::getCollectPostId, event.postId())
                .eq(PostCollect::getCollectUserId, event.userId()));
        if (rows == 0) {
            log.info("收藏记录不存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        addRankScore(event.postId(), -RankConstants.WEIGHT_FAVORITE);
    }

    /**
     * 功能：给当日热度榜加分（delta 允许为负），失败仅记录日志不补偿。
     *
     * <p>榜单是可再生展示数据，最坏少记几分无感；每次加分后重设 TTL，
     * 使日榜在当天最后一次互动后仍保留 7 天供回溯。
     *
     * @param postId 帖子 ID
     * @param delta 热度分增量，正为加分，负为扣分
     */
    private void addRankScore(Long postId, double delta) {
        try {
            String rankKey = RankConstants.todayKey();
            redisService.zIncrementScore(rankKey, String.valueOf(postId), delta);
            redisService.expire(rankKey, RankConstants.RANK_TTL);
        } catch (Exception exception) {
            log.error("榜单加分失败，容忍少量偏差，postId={}，delta={}", postId, delta, exception);
        }
    }
}
