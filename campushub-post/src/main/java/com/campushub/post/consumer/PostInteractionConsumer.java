package com.campushub.post.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.post.constant.PostRedisKeys;
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
 * 功能：帖子互动事件消费者，把点赞/收藏明细落库到 ch_like / ch_collect，
 * 并把聚合计数净增量累加到 Redis delta Hash，由定时任务批量刷库与榜单加权。
 *
 * <p>运行在 Spring AMQP 容器线程池上，AUTO 确认模式：方法正常返回自动 ACK，
 * 抛出异常由框架本地重试，重试耗尽后拒绝且不重回队列，经死信路由进入 DLQ 人工处理。
 *
 * <p>职责边界：明细表是互动的事实源（唯一键幂等吸收重复投递），本消费者只写
 * 明细行与计数增量；ch_post 聚合计数 UPDATE 与热度榜加分统一后移到
 * InteractionCountFlushTask，在增量落库成功后批量执行，避免逐事件打库。
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
     * @throws Exception 明细落库失败时抛出，触发容器本地重试与死信兜底
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

    /** 插入点赞明细，唯一键冲突说明重复消息，幂等丢弃；成功后点赞计数增量 +1。 */
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
        accumulateDelta(PostRedisKeys.LIKE_DELTA_KEY, event.postId(), 1);
    }

    /** 删除点赞明细，记录不存在说明重复消息，幂等丢弃；删除成功后点赞计数增量 -1。 */
    private void unlike(PostInteractionEvent event) {
        int rows = postLikeMapper.delete(Wrappers.<PostLike>lambdaQuery()
                .eq(PostLike::getLikePostId, event.postId())
                .eq(PostLike::getLikeUserId, event.userId()));
        if (rows == 0) {
            // 记录不存在：重复的取消消息，不重复扣减计数
            log.info("点赞记录不存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        accumulateDelta(PostRedisKeys.LIKE_DELTA_KEY, event.postId(), -1);
    }

    /** 插入收藏明细，幂等语义与点赞一致；成功后收藏计数增量 +1。 */
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
        accumulateDelta(PostRedisKeys.FAV_DELTA_KEY, event.postId(), 1);
    }

    /** 删除收藏明细，幂等语义与取消点赞一致；删除成功后收藏计数增量 -1。 */
    private void uncollect(PostInteractionEvent event) {
        int rows = postCollectMapper.delete(Wrappers.<PostCollect>lambdaQuery()
                .eq(PostCollect::getCollectPostId, event.postId())
                .eq(PostCollect::getCollectUserId, event.userId()));
        if (rows == 0) {
            log.info("收藏记录不存在，幂等丢弃，postId={}，userId={}", event.postId(), event.userId());
            return;
        }
        accumulateDelta(PostRedisKeys.FAV_DELTA_KEY, event.postId(), -1);
    }

    /**
     * 功能：把互动净增量累加到聚合计数 Hash，等待定时任务批量刷库。
     *
     * <p>计数是展示型冗余数据（at-most-once）：Redis 写入失败仅记日志不抛出，
     * 抛出重试也无意义——消息重投时明细插入会命中唯一键幂等丢弃，增量不会补发；
     * 需要精确值时以明细表 COUNT 为准重新校准。
     *
     * @param deltaKey 聚合计数 Hash 键
     * @param postId 帖子 ID
     * @param delta 净增量，+1 点赞/收藏，-1 取消
     */
    private void accumulateDelta(String deltaKey, Long postId, int delta) {
        try {
            redisService.hIncrBy(deltaKey, String.valueOf(postId), delta);
        } catch (Exception exception) {
            log.error("互动计数增量写入失败，容忍计数偏差，deltaKey={}，postId={}，delta={}",
                    deltaKey, postId, delta, exception);
        }
    }
}
