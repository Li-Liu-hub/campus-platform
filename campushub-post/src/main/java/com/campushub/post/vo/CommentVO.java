package com.campushub.post.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论接口返回对象。
 *
 * <p>一级评论的 {@code replies} 携带其全部二级回复（按时间正序，对话流）；
 * 二级回复的 {@code replies} 恒为空列表。用 @Data 类而非 record：
 * 回复树需要可变 children 结构，由服务层组装。
 */
@Data
public class CommentVO {

    /** 评论 ID。 */
    private Long commentId;

    /** 所属帖子 ID。 */
    private Long postId;

    /** 评论者用户 ID。 */
    private Long sentUserId;

    /** 评论者昵称，评论者已注销时为空。 */
    private String sentUserNickname;

    /** 父评论 ID：0 表示一级评论。 */
    private Long fatherId;

    /** 被回复用户 ID：一级评论为帖子作者，二级回复为被回复评论的作者。 */
    private Long receiveUserId;

    /** 被回复用户昵称，用于渲染"回复 @某人"。 */
    private String receiveUserNickname;

    /** 评论内容。 */
    private String commentText;

    /** 评论时间。 */
    private LocalDateTime createTime;

    /** 二级回复列表：一级评论携带，按时间正序；二级回复恒为空列表。 */
    private List<CommentVO> replies = new ArrayList<>();
}
