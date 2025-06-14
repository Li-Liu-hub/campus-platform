package com.campushub.post.constant;

import java.util.Set;

/** 帖子允许使用的分类。 */
public final class PostCategories {

    /** 系统支持的帖子分类名称。 */
    public static final Set<String> VALUES = Set.of(
            "打听求助",
            "校园吐槽",
            "二手闲置",
            "失物招领",
            "恋爱交友",
            "游戏开黑",
            "其他内容"
    );

    /** 常量类，禁止实例化。 */
    private PostCategories() {
    }
}
