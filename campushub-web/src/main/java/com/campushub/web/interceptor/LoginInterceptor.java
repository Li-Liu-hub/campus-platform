package com.campushub.web.interceptor;

import com.campushub.infrastructure.security.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** 统一校验需要登录的 Web 请求。 */
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthenticationService authenticationService;

    public LoginInterceptor(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 功能：校验当前请求携带的登录凭证。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前请求对应的处理器
     * @return 登录校验通过后继续执行后续处理器
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        authenticationService.checkLogin();
        return true;
    }
}
