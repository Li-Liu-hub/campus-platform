package com.campushub.infrastructure.mq.impl;

import com.campushub.infrastructure.mq.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** 基于 RabbitTemplate 的消息发布实现。 */
@Service
@RequiredArgsConstructor
public class RabbitMessagePublisherImpl implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /** 将消息以 JSON 格式发送到指定交换机。 */
    @Override
    public void publish(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
