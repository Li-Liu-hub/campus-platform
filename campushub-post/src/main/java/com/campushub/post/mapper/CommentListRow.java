package com.campushub.post.mapper;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论查询行：评论字段 + 评论者/被回复者昵称，由 JOIN 查询一次组装。
 *
 * <p>非表结构载体，仅用于承接 {@link PostCommentMapper} 的列表查询结果。
 */
@Data
public class CommentListRow {

    /** 评论 ID。 */
    private Long commentId;

    /** 所属帖子 ID。 */
    private Long commentPostId;

    /** 评论者用户 ID。 */
    private Long commentSentUserId;

    /** 评论者昵称。 */
    private String sentUserNickname;

    /** 父评论 ID：0 表示一级评论。 */
    private Long commentFatherId;

    /** 被回复用户 ID。 */
    private Long commentReceiveUserId;

    /** 被回复用户昵称。 */
    private String receiveUserNickname;

    /** 评论内容。 */
    private String commentText;

    /** 评论时间。 */
    private LocalDateTime createTime;
}
