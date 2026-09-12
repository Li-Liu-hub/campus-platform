package com.campushub.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发送消息请求参数。
 *
 * <p>消息内容与图片列表至少提供其一（由服务层校验）：纯文本、纯图片、
 * 文字加图片都合法，全空视为无效消息。
 */
public record MessageSendRequest(
        @NotNull(message = "会话 ID 不能为空")
        Long conversationId,

        @Size(max = 2000, message = "消息内容长度不能超过 2000 个字符")
        String messageText,

        @Size(max = 9, message = "单条消息最多携带 9 张图片")
        List<@NotBlank(message = "图片地址不能为空")
             @Size(max = 512, message = "图片地址长度不能超过 512 个字符") String> imageUrls,

        @NotBlank(message = "消息幂等键不能为空")
        @Size(max = 64, message = "消息幂等键长度不能超过 64 个字符")
        String messageIdempotencyKey
) {
}
