package com.campushub.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * 功能：聊天通道的 WebSocket 处理器，负责连接注册、心跳应答与断开清理。
 *
 * <p>职责边界：上行只保留心跳帧（ping → pong）。业务消息发送刻意不走 WebSocket——
 * HTTP 链路能复用登录拦截、幂等键、限流与统一异常，WS 帧发消息这些都要重新实现，
 * 因此本处理器收到其他上行内容只记日志忽略，引导客户端走 HTTP 接口。
 *
 * <p>连接竞态：建立时用 {@link ConcurrentWebSocketSessionDecorator} 包装会话再注册，
 * 使「业务线程推送」与「IO 线程心跳应答」对同一连接的并发写在装饰器内排队，
 * 避免并发写帧异常；超时或缓冲超限的慢客户端由装饰器直接断开。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** 单帧发送排队超时毫秒数：超过该时长未能写出视为慢连接，由装饰器断开。 */
    private static final int SEND_TIME_LIMIT_MS = 5000;

    /** 单连接待发送缓冲上限字节数：推送堆积超过该值说明客户端消费过慢，由装饰器断开背压。 */
    private static final int SESSION_BUFFER_LIMIT_BYTES = 512 * 1024;

    /** 装饰器在会话属性中的键：心跳应答需要复用同一个装饰器，保证与推送共用同一把发送锁。 */
    private static final String DECORATED_SESSION_ATTRIBUTE = ChatWebSocketHandler.class.getName() + ".decorated";

    private final WsSessionRegistry sessionRegistry;

    /** 连接建立：包装为并发安全会话后注册，用户可多端在线，连接追加保存。 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = loginUserId(session);
        if (userId == null) {
            // 兜底：握手拦截器已保证 userId 存在，此处防御任何绕过拦截器的裸连接
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        WebSocketSession decorated =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SESSION_BUFFER_LIMIT_BYTES);
        session.getAttributes().put(DECORATED_SESSION_ATTRIBUTE, decorated);
        sessionRegistry.register(userId, decorated);
        log.info("WebSocket 连接建立，userId={}，sessionId={}", userId, session.getId());
    }

    /** 上行消息处理：仅响应心跳，其余内容记日志忽略（业务发送走 HTTP）。 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        if (WsConstants.PING.equals(payload)) {
            decoratedSession(session).sendMessage(new TextMessage(WsConstants.PONG));
            return;
        }
        log.warn("收到未支持的 WebSocket 上行消息，已忽略（业务发送请走 HTTP 接口），sessionId={}，payload={}",
                session.getId(), payload);
    }

    /** 连接关闭：按 sessionId 从注册表移除，最后一条连接的移除会清理用户维度条目。 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return;
        }
        sessionRegistry.remove(userId, session.getId());
        log.info("WebSocket 连接关闭，userId={}，sessionId={}，status={}", userId, session.getId(), status);
    }

    /** 读取握手阶段绑定到会话属性中的登录用户 ID（原始会话与装饰器共享同一属性表）。 */
    private Long loginUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(WsConstants.USER_ID_ATTRIBUTE);
        return value instanceof Long userId ? userId : null;
    }

    /** 取注册时保存的装饰器会话用于发送；取不到时退回原始会话，保证心跳始终可达。 */
    private WebSocketSession decoratedSession(WebSocketSession session) {
        Object decorated = session.getAttributes().get(DECORATED_SESSION_ATTRIBUTE);
        return decorated instanceof WebSocketSession webSocketSession ? webSocketSession : session;
    }

    /** 关闭连接并吞掉关闭异常，仅记录日志。 */
    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException exception) {
            log.warn("关闭异常 WebSocket 连接失败，sessionId={}", session.getId(), exception);
        }
    }
}
