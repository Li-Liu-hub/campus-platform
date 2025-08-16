package com.campushub.post.consumer;

/**
 * 缓存删除指令，由帖子更新路径在业务删除之后发布，经停车队列 TTL 过期死信改道后，
 * 由消费者执行延迟双删的第二次删除。
 *
 * @param postId 帖子 ID，不允许为空
 */
public record CacheEvictEvent(Long postId) {
}
