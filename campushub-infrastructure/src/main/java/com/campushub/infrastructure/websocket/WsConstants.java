package com.campushub.infrastructure.websocket;

/**
 * WebSocket 通道常量：端点路径、握手参数与会话属性键的唯一定义处。
 *
 * <p>与 MQ 的命名体系同思路——名字集中收口，生产者与消费者引用同一常量，
 * 裸字符串消失使 typo 从源头消灭。
 */
public final class WsConstants {

    /** WebSocket 端点路径。 */
    public static final String ENDPOINT = "/ws/chat";

    /** 握手 token 查询参数名：浏览器 WebSocket API 不能自定义请求头，凭证只能走查询参数。 */
    public static final String TOKEN_PARAM = "token";

    /** 握手成功后绑定到会话属性中的登录用户 ID 键，连接生命周期内随会话携带。 */
    public static final String USER_ID_ATTRIBUTE = "wsLoginUserId";

    /** 心跳帧：客户端发送，服务端回复 {@link #PONG} 保活。 */
    public static final String PING = "ping";

    /** 心跳回应帧。 */
    public static final String PONG = "pong";

    /** 常量类，禁止实例化。 */
    private WsConstants() {
    }
}
