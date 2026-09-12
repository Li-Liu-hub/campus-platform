package com.campushub.infrastructure.websocket;

/**
 * 功能：实时推送能力接口，业务模块通过它向在线用户推送事件，不感知 WebSocket 实现细节。
 *
 * <p>职责边界：只做「把事件送到在线连接」这一件事——不查库、不落库、不判断业务语义。
 * 接收方离线时静默不推（消息由业务层落库，靠上线拉取补偿），
 * 是否需要落库、落什么库由业务层决策。
 */
public interface RealtimePushService {

    /** 判断用户当前是否有在线连接。 */
    boolean isOnline(Long userId);

    /**
     * 功能：向指定用户的所有在线连接推送事件，离线时不推且不报错。
     *
     * <p>推送失败只记日志，不向调用方抛出——实时推送是尽力送达（best-effort），
     * 数据可靠性由业务层的落库与拉取补偿保证，推送失败不影响主流程。
     *
     * @param userId 目标用户 ID，为空直接忽略
     * @param eventType 事件类型，由业务模块定义（如 MESSAGE）
     * @param payload 事件数据体，任意可 JSON 序列化的对象
     */
    void sendToUser(Long userId, String eventType, Object payload);
}
