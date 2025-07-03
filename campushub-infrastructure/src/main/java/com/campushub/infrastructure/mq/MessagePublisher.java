package com.campushub.infrastructure.mq;

/** 消息发布能力接口，业务模块统一通过本接口发送消息，不直接依赖 RabbitTemplate。 */
public interface MessagePublisher {

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机，业务标识自动生成。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     */
    void publish(String exchange, String routingKey, Object message);

    /**
     * 功能：将已序列化的 JSON 文本原样发送到指定交换机。
     *
     * <p>供发件箱轮询中继使用：payload 存储时已是 JSON 文本，直接按字节发送，
     * 避免经过对象转换器被二次序列化。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键
     * @param jsonPayload 已序列化的 JSON 文本
     * @param bizId 业务标识，confirm 回执 nack 时用于定位业务消息
     */
    void publishJson(String exchange, String routingKey, String jsonPayload, String bizId);

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机，并携带业务标识。
     *
     * <p>业务标识随 CorrelationData 传递，confirm 回执 nack 时凭它定位是哪条业务消息
     * 未被 broker 确认，供人工排查。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     * @param bizId 业务标识，如 "oplog:POST_UPDATE:123"，用于 confirm 失败后的定位
     */
    void publish(String exchange, String routingKey, Object message, String bizId);
}
