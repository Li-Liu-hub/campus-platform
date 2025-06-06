package com.campushub.config;

import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.web.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** WebMVC 配置，统一注册 Sa-Token 登录拦截器。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationService authenticationService;

    public WebMvcConfig(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 功能：为除公开认证接口外的所有请求执行登录状态校验。
     *
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(authenticationService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/error",
                        "/favicon.ico"
                );
    }
}
