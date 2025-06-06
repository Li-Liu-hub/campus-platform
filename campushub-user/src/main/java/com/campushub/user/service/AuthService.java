package com.campushub.user.service;

import com.campushub.infrastructure.security.LoginDevice;
import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.vo.LoginVO;

/** 用户认证业务接口。 */
public interface AuthService {

    /** 注册用户。 */
    void register(RegisterDTO request);

    /** 校验用户登录信息并根据设备信息创建登录状态。 */
    LoginVO login(LoginDTO request, LoginDevice device);

    /** 退出当前登录状态。 */
    void logout();
}
