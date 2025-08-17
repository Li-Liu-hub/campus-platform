package com.campushub.post.constant;

/** 帖子域 Redis 键集中定义，生产者与任务/消费者统一引用，避免裸字符串 typo。 */
public final class PostRedisKeys {

    /** 帖子详情缓存键前缀，完整键为 campushub:post:info:{postId}，榜单组装与详情读取共用。 */
    public static final String INFO_KEY_PREFIX = "campushub:post:info:";

    /** 浏览增量聚合 Hash 键，field 为帖子 ID，value 为未落库的浏览增量。 */
    public static final String VIEW_DELTA_KEY = "campushub:post:view:delta";

    /** 点赞计数增量聚合 Hash 键，field 为帖子 ID，value 为未落库的点赞净增量，消费者写入、定时任务刷库。 */
    public static final String LIKE_DELTA_KEY = "campushub:post:like:delta";

    /** 收藏计数增量聚合 Hash 键，field 为帖子 ID，value 为未落库的收藏净增量，消费者写入、定时任务刷库。 */
    public static final String FAV_DELTA_KEY = "campushub:post:fav:delta";

    /** 帖子点赞用户集合键前缀，完整键为 campushub:post:like:members:{postId}，member 为用户 ID。 */
    public static final String LIKE_MEMBERS_KEY_PREFIX = "campushub:post:like:members:";

    /** 帖子收藏用户集合键前缀，完整键为 campushub:post:collect:members:{postId}，member 为用户 ID。 */
    public static final String COLLECT_MEMBERS_KEY_PREFIX = "campushub:post:collect:members:";

    /** 常量类，禁止实例化。 */
    private PostRedisKeys() {
    }
}
