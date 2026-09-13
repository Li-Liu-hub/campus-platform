package com.campushub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改密码请求参数。 */
public record UpdatePasswordRequest(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "新密码长度为 6 到 64 个字符")
        String newPassword
) {
}
