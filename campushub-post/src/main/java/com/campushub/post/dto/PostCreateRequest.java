package com.campushub.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 新增帖子请求参数。 */
public record PostCreateRequest(
        @NotBlank(message = "帖子标题不能为空")
        @Size(max = 128, message = "帖子标题长度不能超过 128 个字符")
        String postTitle,

        @NotBlank(message = "帖子类型不能为空")
        @Size(max = 32, message = "帖子类型长度不能超过 32 个字符")
        String postType,

        @NotBlank(message = "帖子内容不能为空")
        @Size(max = 65535, message = "帖子内容长度不能超过 65535 个字符")
        String postText,

        /** 图片地址列表（先经 /api/v1/files/upload 上传得到），最多 9 张，可为空 */
        @Size(max = 9, message = "单帖最多携带 9 张图片")
        List<@NotBlank(message = "图片地址不能为空")
             @Size(max = 512, message = "图片地址长度不能超过 512 个字符") String> imageUrls,

        @NotBlank(message = "帖子幂等键不能为空")
        @Size(max = 64, message = "帖子幂等键长度不能超过 64 个字符")
        String postIdempotencyKey
) {
}
