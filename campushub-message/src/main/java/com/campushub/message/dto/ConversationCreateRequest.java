package com.campushub.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建会话请求参数。
 *
 * <p>语义为「获取或创建」：一对用户之间只会存在一个会话，
 * 重复提交（换幂等键或对方反向发起）都会返回已存在的原会话。
 */
public record ConversationCreateRequest(
        @NotNull(message = "目标用户 ID 不能为空")
        Long targetUserId,

        @NotBlank(message = "会话幂等键不能为空")
        @Size(max = 64, message = "会话幂等键长度不能超过 64 个字符")
        String conversationIdempotencyKey
) {
}
