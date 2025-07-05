package com.campushub.post.service;

/** 帖子点赞/收藏接口，写路径为 Redis 判重 + MQ 异步落库，接口返回不代表已落库。 */
public interface PostInteractionService {

    /**
     * 功能：点赞帖子，Redis 判重后发布点赞事件，由消费者异步落库并更新榜单。
     *
     * @param postId 帖子 ID，不允许为空且帖子必须存在
     * @throws com.campushub.common.exception.BusinessException 帖子不存在时抛 404，已点赞时抛 409
     */
    void like(Long postId);

    /**
     * 功能：取消点赞，发布取消事件由消费者异步删除记录并扣减榜单分。
     *
     * @param postId 帖子 ID，不允许为空
     * @throws com.campushub.common.exception.BusinessException 尚未点赞时抛 409
     */
    void unlike(Long postId);

    /**
     * 功能：收藏帖子，语义与点赞同构。
     *
     * @param postId 帖子 ID，不允许为空且帖子必须存在
     * @throws com.campushub.common.exception.BusinessException 帖子不存在时抛 404，已收藏时抛 409
     */
    void collect(Long postId);

    /**
     * 功能：取消收藏，语义与取消点赞同构。
     *
     * @param postId 帖子 ID，不允许为空
     * @throws com.campushub.common.exception.BusinessException 尚未收藏时抛 409
     */
    void uncollect(Long postId);
}
