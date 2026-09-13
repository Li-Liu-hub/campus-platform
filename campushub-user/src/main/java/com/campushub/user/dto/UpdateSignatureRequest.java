package com.campushub.user.dto;

import jakarta.validation.constraints.Size;

/** 修改个性签名请求参数。 */
public record UpdateSignatureRequest(
        /** 新的个性签名：允许为空（清空签名），最长 255 个字符 */
        @Size(max = 255, message = "个性签名长度不能超过 255 个字符")
        String userPersonalSignature
) {
}
