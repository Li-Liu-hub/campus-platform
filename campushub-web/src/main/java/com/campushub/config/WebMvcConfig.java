package com.campushub.config;

import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.web.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** WebMVC 配置，统一注册 Sa-Token 登录拦截器与上传目录的静态资源映射。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationService authenticationService;

    /** 本地上传目录（与 ObjectStorageService 本地实现同一配置项）。 */
    @Value("${campushub.storage.local.root-dir:./uploads}")
    private String storageRootDir;

    /** 本地上传 URL 前缀。 */
    @Value("${campushub.storage.local.url-prefix:/files}")
    private String storageUrlPrefix;

    public WebMvcConfig(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 功能：为除公开认证接口、WebSocket 握手与接口文档外的所有请求执行登录状态校验。
     *
     * <p>放行 /ws/**：WebSocket 握手以查询参数携带 token（浏览器 WS API 无法自定义请求头），
     * 无法被基于 HTTP 头的登录拦截器校验，改由握手拦截器（WsHandshakeInterceptor）自行认证。
     *
     * <p>放行 /v3/api-docs/** 与 /swagger-ui/**：接口文档需要免登录访问供 Apifox 导入；
     * 上线前应改为仅开发环境开放或加访问控制。
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
                        "/favicon.ico",
                        "/ws/**",
                        "/files/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }

    /**
     * 功能：把本地上传目录映射为静态资源，使上传接口返回的 URL 可直接访问。
     *
     * <p>图片属于公开资源（<img> 标签无法携带 Authorization 头），因此同时在
     * 拦截器配置中放行了 /files/**；接入 OSS 后本映射可移除。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageRootDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(storageUrlPrefix + "/**").addResourceLocations(location);
    }
}
