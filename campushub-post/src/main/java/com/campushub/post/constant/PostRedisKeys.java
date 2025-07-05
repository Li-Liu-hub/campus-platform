package com.campushub.post.constant;

/** 帖子域 Redis 键集中定义，生产者与任务/消费者统一引用，避免裸字符串 typo。 */
public final class PostRedisKeys {

    /** 浏览增量聚合 Hash 键，field 为帖子 ID，value 为未落库的浏览增量。 */
    public static final String VIEW_DELTA_KEY = "campushub:post:view:delta";

    /** 帖子点赞用户集合键前缀，完整键为 campushub:post:like:members:{postId}，member 为用户 ID。 */
    public static final String LIKE_MEMBERS_KEY_PREFIX = "campushub:post:like:members:";

    /** 帖子收藏用户集合键前缀，完整键为 campushub:post:collect:members:{postId}，member 为用户 ID。 */
    public static final String COLLECT_MEMBERS_KEY_PREFIX = "campushub:post:collect:members:";

    /** 常量类，禁止实例化。 */
    private PostRedisKeys() {
    }
}
