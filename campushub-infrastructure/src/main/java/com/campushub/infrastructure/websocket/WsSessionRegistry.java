package com.campushub.infrastructure.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能：在线连接注册中心，维护「用户 ID → 全部在线连接」映射，支撑点对点推送与在线判定。
 *
 * <p>一个用户允许多端同时在线（与 Sa-Token 的 max-login-count 一致），因此内层按
 * sessionId 再建一层映射；推送时遍历该用户的全部连接，任一端的建立与关闭不影响其他端。
 *
 * <p>线程安全：外层与内层都是 ConcurrentHashMap。注册发生在容器 IO 线程（连接建立回调），
 * 读取发生在业务线程（HTTP 请求事务提交后的推送），移除发生在关闭回调线程，三者并发安全。
 */
@Component
public class WsSessionRegistry {

    /**
     * 在线连接表：外层键为用户 ID，内层键为 sessionId。
     * 按 sessionId 而非直接存 Set：关闭回调拿到的是原始 session，需要按 ID 精确移除。
     */
    private final Map<Long, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /** 注册一条已建立的连接，同一用户的连接追加保存。 */
    public void register(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    /** 按 sessionId 移除连接；该用户最后一条连接移除后清理外层条目，防止 Map 随历史用户无限增长。 */
    public void remove(Long userId, String sessionId) {
        userSessions.computeIfPresent(userId, (key, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** 判断用户当前是否有在线连接。 */
    public boolean isOnline(Long userId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /** 获取用户当前全部在线连接的快照，推送时遍历，不与注册、移除操作互相阻塞。 */
    public Set<WebSocketSession> getSessions(Long userId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        return sessions == null ? Set.of() : Set.copyOf(sessions.values());
    }
}
