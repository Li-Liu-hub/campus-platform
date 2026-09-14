package com.campushub.post.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论列表分页返回对象。
 *
 * <p>total 为帖子的一级评论总数（二级回复数随各条 replies 自带），
 * 游标为 (create_time, comment_id) 二元组。
 */
public record CommentPageVO(
        List<CommentVO> list,
        long total,
        boolean hasMore,
        LocalDateTime nextCursorTime,
        Long nextCursorId
) {
}
