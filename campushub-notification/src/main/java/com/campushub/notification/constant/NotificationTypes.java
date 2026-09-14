package com.campushub.notification.constant;

/**
 * 通知类型常量，与 ch_notification.notification_type 取值对齐。
 *
 * <p>类型是前端渲染的契约（按类型选图标/文案模板），触发方
 * （关注、帖子互动等）统一引用本常量，禁止散落裸字符串。
 */
public final class NotificationTypes {

    /** 被关注：有人关注了你。 */
    public static final String FOLLOW = "FOLLOW";

    /** 帖子被点赞。 */
    public static final String LIKE = "LIKE";

    /** 帖子被收藏。 */
    public static final String COLLECT = "COLLECT";

    /** 帖子被评论：评论者评了你的帖子。 */
    public static final String COMMENT = "COMMENT";

    /** 评论被回复：有人回复了你的评论。 */
    public static final String REPLY = "REPLY";

    /** 常量类，禁止实例化。 */
    private NotificationTypes() {
    }
}
