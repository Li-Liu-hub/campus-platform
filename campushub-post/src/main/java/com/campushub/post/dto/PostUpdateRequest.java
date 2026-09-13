package com.campushub.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 修改帖子请求参数。 */
public record PostUpdateRequest(
        @NotBlank(message = "帖子标题不能为空")
        @Size(max = 128, message = "帖子标题长度不能超过 128 个字符")
        String postTitle,

        @NotBlank(message = "帖子类型不能为空")
        @Size(max = 32, message = "帖子类型长度不能超过 32 个字符")
        String postType,

        @NotBlank(message = "帖子内容不能为空")
        @Size(max = 65535, message = "帖子内容长度不能超过 65535 个字符")
        String postText,

        /**
         * 图片地址列表：不传（null）表示不改动图片，传空列表表示清空图片，非空则全量替换。
         *
         * <p>用“传才改”而非整体覆盖：与库存调整同思路，避免调用方不关心图片时
         * 因序列化差异把既有图片意外清空。
         */
        @Size(max = 9, message = "单帖最多携带 9 张图片")
        List<@NotBlank(message = "图片地址不能为空")
             @Size(max = 512, message = "图片地址长度不能超过 512 个字符") String> imageUrls
) {
}
