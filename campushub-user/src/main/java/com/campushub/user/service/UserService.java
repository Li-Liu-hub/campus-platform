package com.campushub.user.service;

import com.campushub.user.vo.UserInfoVO;

/** 用户信息业务接口。 */
public interface UserService {

    /** 查询当前登录用户的个人信息。 */
    UserInfoVO getCurrentUserInfo();
}
