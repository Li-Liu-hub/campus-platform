package com.campushub.post.controller;

import com.campushub.common.response.Result;
import com.campushub.post.service.PostInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 帖子点赞/收藏接口，写路径异步落库，返回成功仅代表事件已受理。 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostInteractionController {

    private final PostInteractionService postInteractionService;

    /** 点赞帖子。 */
    @PostMapping("/{postId}/like")
    public Result<Void> like(@PathVariable Long postId) {
        postInteractionService.like(postId);
        return Result.success();
    }

    /** 取消点赞。 */
    @DeleteMapping("/{postId}/like")
    public Result<Void> unlike(@PathVariable Long postId) {
        postInteractionService.unlike(postId);
        return Result.success();
    }

    /** 收藏帖子。 */
    @PostMapping("/{postId}/collect")
    public Result<Void> collect(@PathVariable Long postId) {
        postInteractionService.collect(postId);
        return Result.success();
    }

    /** 取消收藏。 */
    @DeleteMapping("/{postId}/collect")
    public Result<Void> uncollect(@PathVariable Long postId) {
        postInteractionService.uncollect(postId);
        return Result.success();
    }
}
