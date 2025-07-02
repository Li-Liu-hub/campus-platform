package com.campushub.infrastructure.mq.impl;

import com.campushub.infrastructure.mq.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** 基于 RabbitTemplate 的消息发布实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMessagePublisherImpl implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机，MQ 故障时静默降级。
     *
     * <p>发布失败仅记录本地错误日志，不向调用方抛出异常——消息属于可丢数据，
     * 保证日志类旁路功能不反噬业务主流程。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     */
    @Override
    public void publish(String exchange, String routingKey, Object message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
        } catch (Exception exception) {
            log.error("消息发布失败，exchange={}，routingKey={}", exchange, routingKey, exception);
        }
    }
}
