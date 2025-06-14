package com.campushub.post.vo;

import java.time.LocalDateTime;

/** 帖子接口返回对象。 */
public record PostVO(
        Long postId,
        Long postUserId,
        String postTitle,
        String postType,
        String postText,
        String postIdempotencyKey,
        Long postViewNumber,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
