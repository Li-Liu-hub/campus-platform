package com.campushub.post.service;

import com.campushub.post.dto.CommentCreateRequest;
import com.campushub.post.dto.CommentQueryRequest;
import com.campushub.post.vo.CommentPageVO;
import com.campushub.post.vo.CommentVO;

/**
 * 帖子评论业务接口（两级结构）。
 *
 * <p>一级评论直接挂在帖子下；二级回复平铺挂在被回复评论所属的一级评论下，
 * 不支持三层嵌套。查询返回"一级评论分页 + 各自全部二级回复"的树形结构。
 */
public interface PostCommentService {

    /**
     * 功能：发表评论。
     *
     * <p>不传父评论 ID 为一级评论（被回复人为帖子作者）；传入父评论 ID 为二级回复
     * （被回复人为该评论的作者，若回复的是二级回复则自动归一到其所属一级评论下）。
     * 幂等由数据库唯一键保证；对被回复人（非自己）创建站内通知，通知失败不反噬评论主流程。
     *
     * @param postId 帖子 ID
     * @param request 评论内容、父评论 ID（可空）与幂等键
     * @return 落库后的评论
     */
    CommentVO create(Long postId, CommentCreateRequest request);

    /** 游标分页查询帖子评论：一级评论按时间倒序 + 各自二级回复按时间正序。 */
    CommentPageVO query(Long postId, CommentQueryRequest request);

    /** 删除本人的评论（软删）：一级评论级联软删其下全部二级回复。 */
    void delete(Long commentId);
}
