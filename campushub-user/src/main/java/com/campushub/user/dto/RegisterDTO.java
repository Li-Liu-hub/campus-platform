package com.campushub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用户注册请求参数。 */
@Data
public class RegisterDTO {

    /** 注册账号。 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64 个字符")
    private String userName;

    /** 注册密码，后续保存前必须加密。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须为 6 到 64 个字符")
    private String userPassword;

}
