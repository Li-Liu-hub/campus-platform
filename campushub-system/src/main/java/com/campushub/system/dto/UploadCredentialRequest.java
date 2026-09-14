package com.campushub.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 前端直传凭证请求参数。
 *
 * <p>文件名用于服务端的扩展名白名单校验（与上传同一套规则）；
 * 业务类型决定对象键前缀，同时也是凭证 policy 限制的 key 前缀。
 */
public record UploadCredentialRequest(
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名长度不能超过 255 个字符")
        String fileName,

        /** 业务类型：post/order/misc，为空时按 misc 处理 */
        @Size(max = 16, message = "业务类型长度不能超过 16 个字符")
        String bizType
) {
}
