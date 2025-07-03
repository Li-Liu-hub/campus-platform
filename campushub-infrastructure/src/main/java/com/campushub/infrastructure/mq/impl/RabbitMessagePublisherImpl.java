package com.campushub.infrastructure.mq.impl;

import com.campushub.infrastructure.mq.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 基于 RabbitTemplate 的消息发布实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMessagePublisherImpl implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机，业务标识自动生成。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     */
    @Override
    public void publish(String exchange, String routingKey, Object message) {
        publish(exchange, routingKey, message, UUID.randomUUID().toString());
    }

    /**
     * 功能：将消息以 JSON 格式发送到指定交换机，MQ 故障时静默降级。
     *
     * <p>发送时携带 CorrelationData，confirm 回执 nack 时回调凭业务标识记录
     * error 日志供人工排查。发布失败仅记录本地错误日志，不向调用方抛出异常——
     * 军规：异步消息永不反噬主流程，MQ 挂了接口照常返回，损失仅限于该条消息本身。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键，由交换机类型和队列绑定关系决定消息去向
     * @param message 消息内容，任意可 JSON 序列化的对象，推荐使用 record
     * @param bizId 业务标识，confirm 回执 nack 时用于定位业务消息
     */
    @Override
    public void publish(String exchange, String routingKey, Object message, String bizId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, new CorrelationData(bizId));
        } catch (Exception exception) {
            log.error("消息发布失败，exchange={}，routingKey={}，bizId={}", exchange, routingKey, bizId, exception);
        }
    }

    /**
     * 功能：将已序列化的 JSON 文本原样发送到指定交换机，MQ 故障时静默降级。
     *
     * <p>手动构造 AMQP 消息（JSON content-type + 持久化），payload 不再经过对象转换器，
     * 保证存储的 JSON 文本与实际发送字节一致。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键
     * @param jsonPayload 已序列化的 JSON 文本
     * @param bizId 业务标识，confirm 回执 nack 时用于定位业务消息
     */
    @Override
    public void publishJson(String exchange, String routingKey, String jsonPayload, String bizId) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            Message message = new Message(jsonPayload.getBytes(StandardCharsets.UTF_8), properties);
            rabbitTemplate.send(exchange, routingKey, message, new CorrelationData(bizId));
        } catch (Exception exception) {
            log.error("消息发布失败，exchange={}，routingKey={}，bizId={}", exchange, routingKey, bizId, exception);
        }
    }
}
