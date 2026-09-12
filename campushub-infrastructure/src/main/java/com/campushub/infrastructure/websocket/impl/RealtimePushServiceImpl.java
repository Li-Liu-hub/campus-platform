package com.campushub.infrastructure.websocket.impl;

import com.campushub.infrastructure.websocket.RealtimePushService;
import com.campushub.infrastructure.websocket.WsEnvelope;
import com.campushub.infrastructure.websocket.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/** 基于本地连接注册表的点对点推送实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimePushServiceImpl implements RealtimePushService {

    private final WsSessionRegistry sessionRegistry;

    private final ObjectMapper objectMapper;

    /** 判断用户当前是否有在线连接。 */
    @Override
    public boolean isOnline(Long userId) {
        return userId != null && sessionRegistry.isOnline(userId);
    }

    /**
     * 功能：向目标用户全部在线连接推送 JSON 信封，离线时静默跳过。
     *
     * <p>序列化失败属编程错误（payload 不可 JSON 化），记 error 后放弃本条推送；
     * 单端发送失败不中断其余端，失效连接由关闭回调负责从注册表移除。
     */
    @Override
    public void sendToUser(Long userId, String eventType, Object payload) {
        if (userId == null || eventType == null) {
            return;
        }
        Set<WebSocketSession> targets = sessionRegistry.getSessions(userId);
        if (targets.isEmpty()) {
            // 离线不推：数据已由业务层落库，接收方上线后走拉取补偿
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(new WsEnvelope(eventType, payload));
        } catch (Exception exception) {
            log.error("实时推送序列化失败，userId={}，eventType={}", userId, eventType, exception);
            return;
        }
        TextMessage frame = new TextMessage(json);
        for (WebSocketSession session : targets) {
            try {
                session.sendMessage(frame);
            } catch (Exception exception) {
                log.warn("实时推送失败，单端跳过，userId={}，sessionId={}，eventType={}",
                        userId, session.getId(), eventType, exception);
            }
        }
    }
}
