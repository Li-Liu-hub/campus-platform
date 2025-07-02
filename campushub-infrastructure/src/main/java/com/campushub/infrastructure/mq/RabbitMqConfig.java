package com.campushub.infrastructure.mq;

import com.campushub.common.mq.MqConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 功能：RabbitMQ 拓扑与可靠传输配置，声明操作日志的交换机、队列与死信兜底拓扑，
 * 并注册生产者确认与消息退回回调。
 *
 * <p>可靠传输分段防御：confirm/returns 事后可观测（段①）、常量集中命名防御（段②）、
 * 持久化三件套（段③）、消费者本地重试与 DLQ（段④，yml 配置）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 功能：注册生产者确认与消息退回回调。
     *
     * <p>ConfirmCallback：broker 收到消息并持久化后异步回执。ack=true 一切正常不处理；
     * ack=false 表示 broker 未收到消息，记 error 日志（correlationData 携带业务标识）供人工排查。
     * confirm 是事后知晓不是同步保障，publish 瞬间即返回，真实语义是"尽力发送 + 事后可观测"。
     *
     * <p>ReturnsCallback：消息到达 broker 但路由不到任何队列时退回——几乎必然意味着
     * 交换机名或路由键写错（typo），开发期就该触发，上线后出现即报警级别。
     */
    @PostConstruct
    public void initPublisherCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息未被 broker 确认，correlationId={}，cause={}",
                        correlationData == null ? "unknown" : correlationData.getId(), cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "消息路由失败被退回，exchange={}，routingKey={}，replyCode={}，replyText={}，body={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                returned.getReplyText(), new String(returned.getMessage().getBody(), StandardCharsets.UTF_8)));
    }

    /**
     * 功能：注册 JSON 消息转换器，Spring Boot 会自动将其应用到 RabbitTemplate，
     * 生产者发送对象时序列化为 JSON，消费者按目标类型反序列化。
     *
     * @return JSON 消息转换器
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 声明操作日志交换机，持久化，topic 类型为将来按事件类别分流预留。 */
    @Bean
    public TopicExchange logExchange() {
        return new TopicExchange(MqConstants.LOG_EXCHANGE, true, false);
    }

    /**
     * 声明操作日志队列：durable 保证 broker 重启消息不丢；
     * 绑定死信交换机与 TTL，超时未消费或消费拒绝的消息统一进入死信队列兜底。
     */
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(MqConstants.LOG_QUEUE)
                .deadLetterExchange(MqConstants.LOG_DLX)
                .deadLetterRoutingKey(MqConstants.LOG_DEAD_ROUTING)
                .ttl(7 * 24 * 60 * 60 * 1000)
                .build();
    }

    /** 将日志队列按操作路由键绑定到日志交换机。 */
    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue()).to(logExchange()).with(MqConstants.LOG_ROUTING_KEY);
    }

    /** 声明死信交换机，持久化。 */
    @Bean
    public TopicExchange logDlxExchange() {
        return new TopicExchange(MqConstants.LOG_DLX, true, false);
    }

    /** 声明死信队列，持久化，由人工消费处理异常日志消息。 */
    @Bean
    public Queue logDlqQueue() {
        return QueueBuilder.durable(MqConstants.LOG_DLQ).build();
    }

    /** 将死信队列绑定到死信交换机。 */
    @Bean
    public Binding logDlqBinding() {
        return BindingBuilder.bind(logDlqQueue()).to(logDlxExchange()).with(MqConstants.LOG_DEAD_ROUTING);
    }
}
