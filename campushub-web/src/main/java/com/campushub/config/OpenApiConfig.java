package com.campushub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能：OpenAPI 文档配置——声明文档基础信息与 Bearer 认证方案。
 *
 * <p>springdoc 自动扫描全部 Controller 生成 OpenAPI 3 规范（/v3/api-docs），
 * Apifox 按 URL 导入后即可获得全部接口定义；这里补上 Bearer 安全方案，
 * 导入方自动识别 {Authorization: Bearer <token>} 并在调试时统一携带。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 功能：注册 OpenAPI 文档描述与全局安全方案。
     *
     * @return 带 Bearer 认证声明的 OpenAPI 配置
     */
    @Bean
    public OpenAPI campusHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CampusHub API")
                        .version("v1")
                        .description("校园交易系统接口文档；Apifox 导入地址：/v3/api-docs"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("UUID")
                                .description("登录接口返回的 tokenValue，格式：Bearer <tokenValue>")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
