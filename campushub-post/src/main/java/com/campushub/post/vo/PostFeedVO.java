package com.campushub.post.vo;

import java.time.LocalDateTime;

/** 帖子信息流列表返回对象，仅包含列表展示所需字段，避免正文等大字段拖慢分页响应。 */
public record PostFeedVO(
        Long postId,
        String userNickname,
        String postTitle,
        String postType,
        Long postViewNumber,
        LocalDateTime createTime
) {
}
