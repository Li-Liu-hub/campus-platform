package com.campushub.user.vo;

import lombok.Builder;
import lombok.Data;

/** 用户个人信息返回对象。 */
@Data
@Builder
public class UserInfoVO {

    /** 用户主键。 */
    private Long userId;

    /** 用户名称，取用户昵称，不返回登录用户名。 */
    private String userNickname;

    /** 用户个性签名。 */
    private String userPersonalSignature;
}
