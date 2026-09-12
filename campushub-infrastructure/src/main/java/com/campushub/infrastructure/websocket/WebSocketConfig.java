package com.campushub.infrastructure.websocket;

import com.campushub.infrastructure.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 功能：WebSocket 通道配置，注册聊天端点与握手认证拦截器。
 *
 * <p>放在基础设施模块而非 web 接入层：与 MQ 拓扑配置（RabbitMqConfig）同构——
 * 连接层的配置类归技术能力层，业务模块只依赖 {@link RealtimePushService} 接口，
 * 编译期不接触任何 WebSocket API。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    private final AuthenticationService authenticationService;

    /**
     * 功能：注册 WebSocket 端点与握手拦截器。
     *
     * <p>握手认证不走 HTTP 头（浏览器 WebSocket API 无法自定义请求头），
     * token 以查询参数传入并由 {@link WsHandshakeInterceptor} 校验；
     * 开发期放开跨域来源便于联调，上线前应把 * 收口为前端域名。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, WsConstants.ENDPOINT)
                .addInterceptors(new WsHandshakeInterceptor(authenticationService))
                .setAllowedOriginPatterns("*");
    }
}
