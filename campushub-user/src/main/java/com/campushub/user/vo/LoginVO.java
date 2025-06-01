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
}
