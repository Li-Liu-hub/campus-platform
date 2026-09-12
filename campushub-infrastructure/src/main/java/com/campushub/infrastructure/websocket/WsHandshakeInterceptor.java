package com.campushub.infrastructure.websocket;

import com.campushub.infrastructure.security.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 功能：WebSocket 握手认证拦截器，从查询参数取 Token 校验登录态，
 * 通过后把用户 ID 绑定到会话属性，失败则直接拒绝握手。
 *
 * <p>为何走查询参数而非请求头：浏览器 WebSocket API 不支持自定义请求头，
 * 客户端只能以 {@code /ws/chat?token=xxx} 形式携带凭证；也因此 MVC 的
 * 登录拦截器放行了 /ws/**，握手认证由本拦截器独立完成。
 *
 * <p>运行在容器线程的握手阶段，此时连接尚未建立：校验失败只返回 401 拒绝升级，
 * 不产生任何连接与注册表条目，匿名请求无法占用连接资源。
 */
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthenticationService authenticationService;

    public WsHandshakeInterceptor(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 功能：校验握手请求携带的 Token，通过后把用户 ID 写入会话属性。
     *
     * @param request 握手 HTTP 请求，从查询串读取 token 参数
     * @param response 握手 HTTP 响应，认证失败时置 401 拒绝升级
     * @param wsHandler 目标 WebSocket 处理器
     * @param attributes 会话属性表，认证通过后写入登录用户 ID
     * @return true 表示允许建立连接；false 表示拒绝
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst(WsConstants.TOKEN_PARAM);
        Long userId = authenticationService.getUserIdByToken(token);
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(WsConstants.USER_ID_ATTRIBUTE, userId);
        return true;
    }

    /** 握手完成后回调，无需额外处理。 */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理：认证已在 beforeHandshake 完成
    }
}
