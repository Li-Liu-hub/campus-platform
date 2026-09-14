package com.campushub.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论请求参数。
 *
 * <p>{@code commentFatherId} 为空或 0 表示一级评论（直接评帖子）；
 * 传入一级评论 ID 表示二级回复（平铺挂在被回复评论所属的一级评论下，
 * 不支持三层嵌套——回复的回复同样平铺，与主流社交产品一致）。
 */
public record CommentCreateRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容长度不能超过 1000 个字符")
        String commentText,

        /** 父评论 ID：空/0 为一级评论，传入则为回复该评论 */
        Long commentFatherId,

        @NotBlank(message = "评论幂等键不能为空")
        @Size(max = 64, message = "评论幂等键长度不能超过 64 个字符")
        String commentIdempotencyKey
) {
}
