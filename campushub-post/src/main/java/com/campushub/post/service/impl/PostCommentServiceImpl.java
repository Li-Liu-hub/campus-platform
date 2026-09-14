package com.campushub.post.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.notification.constant.NotificationTypes;
import com.campushub.notification.service.NotificationService;
import com.campushub.post.dto.CommentCreateRequest;
import com.campushub.post.dto.CommentQueryRequest;
import com.campushub.post.entity.Post;
import com.campushub.post.entity.PostComment;
import com.campushub.post.mapper.CommentListRow;
import com.campushub.post.mapper.PostCommentMapper;
import com.campushub.post.mapper.PostMapper;
import com.campushub.post.service.PostCommentService;
import com.campushub.post.vo.CommentPageVO;
import com.campushub.post.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子评论业务实现（两级结构）。
 *
 * <p>层级规则：一级评论 father_id = 0；二级回复一律平铺挂在"一级评论"下——
 * 回复二级回复时自动归一到所属一级评论，任意深度都只呈现两层。
 * 被回复人（receive_user_id）在写入时冗余：一级为帖子作者、二级为被回复评论作者，
 * 通知与"回复 @某人"渲染都不再回查父链。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentMapper postCommentMapper;

    private final PostMapper postMapper;

    private final AuthenticationService authenticationService;

    private final NotificationService notificationService;

    /** 一级评论的父评论 ID 哨兵值。 */
    private static final long ROOT_FATHER_ID = 0L;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 通知文案中帖子标题的截断长度。 */
    private static final int TITLE_ABBREVIATE_LENGTH = 20;

    /**
     * 功能：发表评论，幂等由唯一键保证。
     *
     * <p>层级归一：回复目标若本身是二级回复，父 ID 归一到其所属一级评论（平铺），
     * 被回复人仍记为该条回复的作者。插入成功后回读一次拿数据库生成的时间与双方昵称，
     * 随后对被回复人（非自己）创建通知——通知失败仅记日志，不反噬评论主流程。
     *
     * @param postId 帖子 ID
     * @param request 评论内容、父评论 ID（可空）与幂等键
     * @return 落库后的评论
     * @throws BusinessException 帖子不存在抛 404；被回复评论不存在或不属于该帖抛 404；
     *                           幂等键被他人占用抛 409；登录失效抛 401
     */
    @Override
    @Transactional
    public CommentVO create(Long postId, CommentCreateRequest request) {
        Long userId = requireCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        String commentText = request.commentText().trim();
        Long requestedFatherId = request.commentFatherId() == null ? ROOT_FATHER_ID : request.commentFatherId();
        boolean isReply = requestedFatherId != ROOT_FATHER_ID;
        long fatherId = ROOT_FATHER_ID;
        Long receiveUserId = post.getPostUserId();
        if (isReply) {
            PostComment target = postCommentMapper.selectById(requestedFatherId);
            if (target == null || !postId.equals(target.getCommentPostId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "被回复的评论不存在");
            }
            // 归一：回复二级回复时挂到其所属一级评论下（平铺），被回复人仍是该条回复的作者
            fatherId = target.getCommentFatherId() != null && target.getCommentFatherId() != ROOT_FATHER_ID
                    ? target.getCommentFatherId()
                    : target.getCommentId();
            receiveUserId = target.getCommentSentUserId();
        }

        PostComment comment = new PostComment();
        comment.setCommentPostId(postId);
        comment.setCommentSentUserId(userId);
        comment.setCommentFatherId(fatherId);
        comment.setCommentReceiveUserId(receiveUserId);
        comment.setCommentText(commentText);
        comment.setCommentIdempotencyKey(request.commentIdempotencyKey().trim());
        try {
            postCommentMapper.insert(comment);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突：重复提交，回查原评论返回（本人键才回查，避免他人键碰撞泄露）；
            // 两步均为锁定读（当前读）：普通 SELECT 受 REPEATABLE READ 快照限制，可能读不到并发赢家刚提交的记录/评论行而误报 409；
            // FOR SHARE 与唯一键冲突残留的 S 锁兼容，多个并发输家不会互相升级成 X 锁死锁
            PostComment existing = postCommentMapper.selectOne(Wrappers.<PostComment>lambdaQuery()
                    .eq(PostComment::getCommentSentUserId, userId)
                    .eq(PostComment::getCommentIdempotencyKey, comment.getCommentIdempotencyKey())
                    .last("LIMIT 1 FOR SHARE"));
            CommentListRow existingRow = existing == null ? null
                    : postCommentMapper.selectRowByIdForShare(existing.getCommentId());
            if (existingRow == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "评论幂等键已被使用");
            }
            return toCommentVO(existingRow);
        }
        CommentListRow row = postCommentMapper.selectRowById(comment.getCommentId());
        CommentVO commentVO = toCommentVO(row);
        notifyReceiveUser(post, row, isReply, receiveUserId, userId);
        return commentVO;
    }

    /**
     * 功能：游标分页查询帖子评论。
     *
     * <p>两级各一次查询：一级评论游标分页（(评论时间, 评论 ID) 倒序），
     * 二级回复按当页一级评论 ID 批量 IN 取（时间正序），内存分组组装成树；
     * total 为一级评论总数。
     */
    @Override
    public CommentPageVO query(Long postId, CommentQueryRequest request) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子 ID 不能为空");
        }
        if (postMapper.selectById(postId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        int pageSize = resolvePageSize(request.pageSize());
        List<CommentListRow> parents = postCommentMapper.selectParentPage(
                postId, request.cursorTime(), request.cursorId(), pageSize);
        long total = postCommentMapper.countParentComments(postId);
        if (parents.isEmpty()) {
            return new CommentPageVO(List.of(), total, false, null, null);
        }
        // 当页一级评论的二级回复一次批量取，避免逐条回查（N+1）
        List<Long> parentIds = parents.stream().map(CommentListRow::getCommentId).toList();
        Map<Long, List<CommentListRow>> repliesByFather = postCommentMapper
                .selectRepliesByFatherIds(parentIds).stream()
                .collect(Collectors.groupingBy(CommentListRow::getCommentFatherId));
        List<CommentVO> list = parents.stream().map(parent -> {
            CommentVO parentVO = toCommentVO(parent);
            List<CommentVO> replies = repliesByFather.getOrDefault(parent.getCommentId(), List.of())
                    .stream().map(this::toCommentVO).toList();
            parentVO.setReplies(replies);
            return parentVO;
        }).toList();
        if (parents.size() < pageSize) {
            return new CommentPageVO(list, total, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        CommentListRow lastRow = parents.get(parents.size() - 1);
        return new CommentPageVO(list, total, true, lastRow.getCreateTime(), lastRow.getCommentId());
    }

    /**
     * 功能：删除本人的评论（软删），一级评论级联软删其下全部二级回复。
     *
     * <p>级联是刻意设计：一级评论隐藏后其回复的对话上下文已不存在，
     * 保留孤儿回复只会让列表出现"回复了一个看不见的评论"的悬空数据。
     */
    @Override
    @Transactional
    public void delete(Long commentId) {
        Long userId = requireCurrentUserId();
        if (commentId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论 ID 不能为空");
        }
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        if (!userId.equals(comment.getCommentSentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的评论");
        }
        // @TableLogic 使 deleteById / delete(wrapper) 生成为软删 UPDATE
        postCommentMapper.deleteById(commentId);
        if (comment.getCommentFatherId() != null && comment.getCommentFatherId() == ROOT_FATHER_ID) {
            postCommentMapper.delete(Wrappers.<PostComment>lambdaQuery()
                    .eq(PostComment::getCommentFatherId, commentId));
        }
    }

    /**
     * 功能：给被回复人创建站内通知（自己评论/回复自己不通知）。
     *
     * <p>通知失败仅记日志：评论已落库，通知是提醒不是事实；与互动通知同一降级策略。
     */
    private void notifyReceiveUser(Post post, CommentListRow row, boolean isReply,
                                   Long receiveUserId, Long operatorUserId) {
        if (receiveUserId == null || receiveUserId.equals(operatorUserId)) {
            return;
        }
        try {
            String operatorName = row.getSentUserNickname() == null ? "有人" : row.getSentUserNickname();
            if (isReply) {
                notificationService.create(receiveUserId, NotificationTypes.REPLY, operatorName + " 回复了你的评论");
            } else {
                notificationService.create(receiveUserId, NotificationTypes.COMMENT,
                        operatorName + " 评论了你的帖子《" + abbreviate(post.getPostTitle()) + "》");
            }
        } catch (Exception exception) {
            log.warn("评论通知创建失败，postId={}，commentId={}，receiveUserId={}",
                    post.getPostId(), row.getCommentId(), receiveUserId, exception);
        }
    }

    /** 截断过长的帖子标题，避免通知文本超长。 */
    private String abbreviate(String title) {
        if (title == null) {
            return "";
        }
        return title.length() <= TITLE_ABBREVIATE_LENGTH ? title : title.substring(0, TITLE_ABBREVIATE_LENGTH) + "…";
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 将查询行转换为接口返回对象（replies 默认空列表，由调用方按需填充）。 */
    private CommentVO toCommentVO(CommentListRow row) {
        CommentVO commentVO = new CommentVO();
        commentVO.setCommentId(row.getCommentId());
        commentVO.setPostId(row.getCommentPostId());
        commentVO.setSentUserId(row.getCommentSentUserId());
        commentVO.setSentUserNickname(row.getSentUserNickname());
        commentVO.setFatherId(row.getCommentFatherId());
        commentVO.setReceiveUserId(row.getCommentReceiveUserId());
        commentVO.setReceiveUserNickname(row.getReceiveUserNickname());
        commentVO.setCommentText(row.getCommentText());
        commentVO.setCreateTime(row.getCreateTime());
        return commentVO;
    }
}
