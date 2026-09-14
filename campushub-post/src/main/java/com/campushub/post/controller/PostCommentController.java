package com.campushub.post.controller;

import com.campushub.common.response.Result;
import com.campushub.post.dto.CommentCreateRequest;
import com.campushub.post.dto.CommentQueryRequest;
import com.campushub.post.service.PostCommentService;
import com.campushub.post.vo.CommentPageVO;
import com.campushub.post.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 帖子评论接口（两级结构：评论帖子 / 回复评论）。 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService postCommentService;

    /** 发表评论：不传 commentFatherId 为评论帖子，传入则为回复该评论（二级平铺）。 */
    @PostMapping("/{postId}/comments")
    public Result<CommentVO> create(@PathVariable Long postId,
                                    @Valid @RequestBody CommentCreateRequest request) {
        return Result.success(postCommentService.create(postId, request));
    }

    /** 游标分页查询帖子评论：一级评论（倒序）+ 各自全部二级回复（正序）。 */
    @GetMapping("/{postId}/comments")
    public Result<CommentPageVO> query(@PathVariable Long postId,
                                       @Valid @ModelAttribute CommentQueryRequest request) {
        return Result.success(postCommentService.query(postId, request));
    }

    /** 删除本人的评论（软删）：一级评论级联软删其下全部回复。 */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> delete(@PathVariable Long commentId) {
        postCommentService.delete(commentId);
        return Result.success();
    }
}
