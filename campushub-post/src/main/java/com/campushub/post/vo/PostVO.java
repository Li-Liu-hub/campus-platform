package com.campushub.post.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 帖子接口返回对象。 */
public record PostVO(
        Long postId,
        Long postUserId,
        String postTitle,
        String postType,
        String postText,
        List<String> imageUrls,
        String postIdempotencyKey,
        Long postViewNumber,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
