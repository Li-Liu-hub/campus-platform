package com.campushub.post.consumer;

import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.post.constant.PostRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 功能：缓存删除指令消费者，执行延迟双删的第二次删除。
 *
 * <p>消息在停车队列滞留 500ms 过期后经死信改道进入删除队列，消费时机即业务删除
 * 之后的延迟点。删除幂等，重复投递无副作用；Redis 异常时抛出由容器本地重试，
 * 重试耗尽死信进 DLQ 人工处理。
 *
 * <p>可靠性定位：延迟双删只是收窄"并发读回源旧值写回缓存"脏窗口的加速器，
 * 即使删除指令彻底失败，残留脏缓存也会被详情缓存 TTL（最长 420 秒）到期自愈。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictConsumer {

    private final RedisService redisService;

    /**
     * 功能：消费缓存删除指令并删除帖子详情缓存。
     *
     * @param event 缓存删除指令，容器按 JSON 反序列化
     * @throws Exception Redis 删除失败时抛出，触发容器本地重试与死信兜底
     */
    @RabbitListener(queues = MqConstants.CACHE_DELETE_QUEUE)
    public void onCacheEvict(CacheEvictEvent event) {
        String cacheKey = PostRedisKeys.INFO_KEY_PREFIX + event.postId();
        Boolean deleted = redisService.delete(cacheKey);
        log.info("延迟双删执行，postId={}，cacheKey={}，删除前是否存在={}", event.postId(), cacheKey, deleted);
    }
}
