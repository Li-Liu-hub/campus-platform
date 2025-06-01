package com.campushub.user.service;

import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/** 用户认证业务接口。 */
public interface AuthService {

    /** 注册用户。 */
    void register(RegisterDTO request);

    /** 校验用户登录信息。 */
    LoginVO login(LoginDTO request, HttpServletRequest httpRequest);

    /** 退出当前登录会话。 */
    void logout(HttpServletRequest httpRequest);
}
