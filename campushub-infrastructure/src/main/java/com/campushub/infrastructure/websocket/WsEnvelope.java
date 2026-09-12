package com.campushub.infrastructure.websocket;

/**
 * 实时推送信封：统一「事件类型 + 数据体」结构，客户端按 type 分发处理。
 *
 * @param type 事件类型，由业务模块定义（如 MESSAGE），基础设施不解释其含义
 * @param data 事件数据体，任意可 JSON 序列化的对象
 */
public record WsEnvelope(String type, Object data) {
}
