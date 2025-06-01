package com.campushub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 用户登录请求参数。 */
@Data
public class LoginDTO {

    /** 登录账号。 */
    @NotBlank(message = "用户名不能为空")
    private String userName;

    /** 登录密码。 */
    @NotBlank(message = "密码不能为空")
    private String userPassword;
}
