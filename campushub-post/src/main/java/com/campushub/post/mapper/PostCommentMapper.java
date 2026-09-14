package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 帖子评论表数据访问接口。 */
@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {

    /**
     * 功能：游标分页查询帖子的一级评论（含评论者昵称），按 (评论时间, 评论 ID) 倒序。
     *
     * @param postId 帖子 ID
     * @param cursorTime 上一页末行的评论时间，首页为 null
     * @param cursorId 上一页末行的评论 ID，首页为 null
     * @param pageSize 页大小
     * @return 一级评论列表行
     */
    List<CommentListRow> selectParentPage(@Param("postId") Long postId,
                                          @Param("cursorTime") LocalDateTime cursorTime,
                                          @Param("cursorId") Long cursorId,
                                          @Param("pageSize") int pageSize);

    /**
     * 功能：批量查询一组一级评论下的全部二级回复（含双方昵称），按评论 ID 正序（对话流）。
     *
     * @param fatherIds 一级评论 ID 列表，不允许为空
     * @return 二级回复列表行
     */
    List<CommentListRow> selectRepliesByFatherIds(@Param("fatherIds") List<Long> fatherIds);

    /**
     * 功能：按评论 ID 查询单条评论（含双方昵称），供发表后的回读使用。
     *
     * @param commentId 评论 ID
     * @return 评论行；评论不存在或已删除时返回 null
     */
    CommentListRow selectRowById(@Param("commentId") Long commentId);

    /**
     * 功能：按评论 ID 以锁定读查询单条评论（含双方昵称），供幂等回查使用。
     *
     * <p>锁定读（当前读）绕过 REPEATABLE READ 快照，确保读到并发赢家刚提交的评论行；
     * 普通 SELECT 会复用旧快照读不到该行，使重复提交被误判为“幂等键已被使用”。
     *
     * @param commentId 评论 ID
     * @return 评论行；评论不存在或已删除时返回 null
     */
    CommentListRow selectRowByIdForShare(@Param("commentId") Long commentId);

    /**
     * 功能：统计帖子的一级评论总数。
     *
     * @param postId 帖子 ID
     * @return 未删除的一级评论数
     */
    long countParentComments(@Param("postId") Long postId);
}
