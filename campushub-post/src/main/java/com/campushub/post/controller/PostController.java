package com.campushub.post.controller;

import com.campushub.common.response.Result;
import com.campushub.post.dto.PostCreateRequest;
import com.campushub.post.dto.PostQueryRequest;
import com.campushub.post.dto.PostUpdateRequest;
import com.campushub.post.service.PostService;
import com.campushub.post.vo.PostPageVO;
import com.campushub.post.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 帖子基础 CRUD 接口。 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 创建帖子。 */
    @PostMapping("/create")
    public Result<PostVO> create(@Valid @RequestBody PostCreateRequest request) {
        return Result.success(postService.create(request));
    }

    /** 按条件游标分页查询帖子，翻页时传上一页返回的游标二元组。 */
    @GetMapping("/query")
    public Result<PostPageVO> query(@Valid @ModelAttribute PostQueryRequest request) {
        return Result.success(postService.query(request));
    }

    /** 浏览帖子：返回帖子详情，浏览量加一。 */
    @GetMapping("/get/{postId}")
    public Result<PostVO> getById(@PathVariable Long postId) {
        return Result.success(postService.view(postId));
    }

    /** 修改帖子。 */
    @PutMapping("/update/{postId}")
    public Result<PostVO> update(@PathVariable Long postId,
                                 @Valid @RequestBody PostUpdateRequest request) {
        return Result.success(postService.update(postId, request));
    }

    /** 软删除帖子。 */
    @DeleteMapping("/delete/{postId}")
    public Result<Void> delete(@PathVariable Long postId) {
        postService.delete(postId);
        return Result.success();
    }
}
