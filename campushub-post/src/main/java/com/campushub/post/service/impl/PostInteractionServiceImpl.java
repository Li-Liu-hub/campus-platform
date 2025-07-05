package com.campushub.post.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.mq.MessagePublisher;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.post.constant.PostRedisKeys;
import com.campushub.post.consumer.PostInteractionEvent;
import com.campushub.post.service.PostInteractionService;
import com.campushub.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 功能：帖子点赞/收藏实现，写路径为"Redis Set 判重 → 发布 MQ 事件"，接口线程零数据库写。
 *
 * <p>可靠性定位与操作日志一致（中等可靠）：durable 拓扑 + 持久化消息 + 本地重试 + DLQ 兜底，
 * 极端情况下允许单条事件丢失（Redis 标记已写而消息丢失时表现为"已点赞但无落库记录"，
 * 等判重标记过期后可重新操作自愈）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostService postService;

    private final AuthenticationService authenticationService;

    private final RedisService redisService;

    private final MessagePublisher messagePublisher;

    /** 判重集合 TTL：集合是 DB 唯一键的副本标记，丢失后由唯一键兜底，设短些尽快自愈。 */
    private static final Duration MEMBER_SET_TTL = Duration.ofDays(7);

    /**
     * 功能：点赞帖子。Redis SAdd 判重（返回 0 说明已点赞），通过后发布点赞事件。
     *
     * <p>Redis 异常时降级放行：判重标记是 DB 唯一键的副本，丢失不影响正确性，
     * 重复事件会被消费者唯一键冲突分支幂等丢弃。
     *
     * @param postId 帖子 ID，不允许为空
     * @throws BusinessException 帖子不存在抛 404，已点赞抛 409
     */
    @Override
    public void like(Long postId) {
        Long userId = requireCurrentUserId();
        // 帖子存在性校验走详情缓存（命中时 MySQL 无感知），不存在直接 404
        postService.getById(postId);
        Long added = addMemberSafely(PostRedisKeys.LIKE_MEMBERS_KEY_PREFIX + postId, String.valueOf(userId));
        if (added != null && added == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已点赞该帖子");
        }
        publish(PostInteractionEvent.Action.LIKE, postId, userId);
    }

    /**
     * 功能：取消点赞。Redis SRem 判重（返回 0 说明尚未点赞），通过后发布取消事件。
     *
     * @param postId 帖子 ID，不允许为空
     * @throws BusinessException 尚未点赞抛 409
     */
    @Override
    public void unlike(Long postId) {
        Long userId = requireCurrentUserId();
        Long removed = removeMemberSafely(PostRedisKeys.LIKE_MEMBERS_KEY_PREFIX + postId, String.valueOf(userId));
        if (removed != null && removed == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "尚未点赞该帖子");
        }
        publish(PostInteractionEvent.Action.UNLIKE, postId, userId);
    }

    /** 收藏帖子，语义与点赞同构，仅集合前缀与事件类型不同。 */
    @Override
    public void collect(Long postId) {
        Long userId = requireCurrentUserId();
        postService.getById(postId);
        Long added = addMemberSafely(PostRedisKeys.COLLECT_MEMBERS_KEY_PREFIX + postId, String.valueOf(userId));
        if (added != null && added == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已收藏该帖子");
        }
        publish(PostInteractionEvent.Action.COLLECT, postId, userId);
    }

    /** 取消收藏，语义与取消点赞同构。 */
    @Override
    public void uncollect(Long postId) {
        Long userId = requireCurrentUserId();
        Long removed = removeMemberSafely(PostRedisKeys.COLLECT_MEMBERS_KEY_PREFIX + postId, String.valueOf(userId));
        if (removed != null && removed == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "尚未收藏该帖子");
        }
        publish(PostInteractionEvent.Action.UNCOLLECT, postId, userId);
    }

    /** 向判重集合添加成员并续期 TTL，返回实际新增数量；Redis 异常时返回 null 表示降级放行。 */
    private Long addMemberSafely(String key, String member) {
        try {
            Long added = redisService.sAdd(key, member);
            redisService.expire(key, MEMBER_SET_TTL);
            return added;
        } catch (Exception exception) {
            log.warn("点赞/收藏判重标记写入失败，降级放行，key={}，member={}", key, member, exception);
            return null;
        }
    }

    /** 从判重集合移除成员，返回实际移除数量；Redis 异常时返回 null 表示降级放行。 */
    private Long removeMemberSafely(String key, String member) {
        try {
            return redisService.sRemove(key, member);
        } catch (Exception exception) {
            log.warn("点赞/收藏判重标记移除失败，降级放行，key={}，member={}", key, member, exception);
            return null;
        }
    }

    /**
     * 功能：发布互动事件到帖子域交换机。
     *
     * <p>发布失败由 publisher 内部静默降级（异步消息永不反噬主流程），
     * bizId 携带完整互动标识，confirm nack 时可凭它定位业务消息。
     *
     * @param action 互动动作
     * @param postId 帖子 ID
     * @param userId 用户 ID
     */
    private void publish(PostInteractionEvent.Action action, Long postId, Long userId) {
        messagePublisher.publish(
                MqConstants.POST_EXCHANGE,
                MqConstants.POST_INTERACTION_ROUTING,
                new PostInteractionEvent(postId, userId, action),
                "post:interaction:" + action + ":" + postId + ":" + userId);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }
}
