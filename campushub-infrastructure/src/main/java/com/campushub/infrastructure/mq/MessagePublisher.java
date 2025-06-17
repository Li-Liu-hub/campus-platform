package com.campushub.infrastructure.mq;

/** 消息发布能力接口，业务模块统一通过本接口发送消息，不直接依赖 RabbitTemplate。 */
public interface MessagePublisher {

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     */
    void publish(String exchange, String routingKey, Object message);
}
