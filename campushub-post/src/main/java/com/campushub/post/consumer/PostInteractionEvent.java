package com.campushub.post.consumer;

/**
 * 帖子互动事件（点赞/收藏及其取消），由业务接口发布，消费者异步落库并在落库成功后更新热度榜。
 *
 * @param postId 帖子 ID，不允许为空
 * @param userId 发起互动的用户 ID，不允许为空
 * @param action 互动动作类型
 */
public record PostInteractionEvent(Long postId, Long userId, Action action) {

    /** 互动动作类型：四种动作共用一条队列，消费者按类型分发。 */
    public enum Action {
        /** 点赞。 */
        LIKE,
        /** 取消点赞。 */
        UNLIKE,
        /** 收藏。 */
        COLLECT,
        /** 取消收藏。 */
        UNCOLLECT
    }
}
