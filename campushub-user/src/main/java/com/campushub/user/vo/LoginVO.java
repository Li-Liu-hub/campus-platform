package com.campushub.user.vo;

import lombok.Builder;
import lombok.Data;

/** 用户登录成功后的返回对象。 */
@Data
@Builder
public class LoginVO {

    /** 用户主键。 */
    private Long userId;

    /** 用户登录账号。 */
    private String userName;

    /** 用户昵称。 */
    private String userNickname;

    /** Sa-Token 登录凭证，后续请求需通过 Authorization: Bearer <tokenValue> 请求头携带。 */
    private String tokenValue;
}
