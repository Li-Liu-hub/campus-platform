package com.campushub.user.controller;

import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.service.AuthService;
import com.campushub.user.vo.LoginVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户认证相关接口，包括登录、注册和退出。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 功能：校验用户登录信息并创建登录会话。
     *
     * @param request 登录账号和密码，账号与密码均不允许为空
     * @return 登录成功后的用户信息
     */
    @PostMapping("/login")
    public LoginVO login(@Valid @RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    /**
     * 功能：校验注册信息并创建用户账号。
     *
     * @param request 注册账号和密码，账号长度不超过 64 个字符，密码长度为 6 到 64 个字符
     */
    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterDTO request) {
        authService.register(request);
    }

    /**
     * 功能：注销当前用户的登录会话，使当前令牌失效。
     */
    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
    }
}
