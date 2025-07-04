package com.campushub.infrastructure.mq;

import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.mq.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
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

    private final OutboxService outboxService;

    /**
     * 功能：注册生产者确认与消息退回回调。
     *
     * <p>ConfirmCallback：broker 收到消息并持久化后异步回执。ack=true 时日志类消息不做任何事；
     * 发件箱消息（correlationId 带 outbox: 前缀）标记已确认，等待保留期清理。
     * ack=false 表示 broker 未收到消息，记 error 日志（correlationData 携带业务标识）供人工排查；
     * 发件箱消息保持待确认状态由轮询中继自动重发。
     * confirm 是事后知晓不是同步保障，publish 瞬间即返回，真实语义是"尽力发送 + 事后可观测"。
     *
     * <p>ReturnsCallback：消息到达 broker 但路由不到任何队列时退回——几乎必然意味着
     * 交换机名或路由键写错（typo），开发期就该触发，上线后出现即报警级别。
     *
     * <p>通过 RabbitTemplateCustomizer 注入回调而非直接依赖 RabbitTemplate：本配置类
     * 同时定义了 MessageConverter Bean，rabbitTemplate 创建过程需要读取该转换器，
     * 若本类构造器注入 RabbitTemplate 会形成 rabbitTemplate → MessageConverter →
     * 本配置类 → rabbitTemplate 的循环依赖。
     */
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer() {
        return rabbitTemplate -> {
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    markOutboxConfirmed(correlationData);
                    return;
                }
                log.error("消息未被 broker 确认，correlationId={}，cause={}",
                        correlationData == null ? "unknown" : correlationData.getId(), cause);
            });
            rabbitTemplate.setReturnsCallback(returned -> log.error(
                    "消息路由失败被退回，exchange={}，routingKey={}，replyCode={}，replyText={}，body={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                    returned.getReplyText(), new String(returned.getMessage().getBody(), StandardCharsets.UTF_8)));
        };
    }

    /** 发件箱消息 confirm 后标记已确认，非发件箱消息不做任何处理。 */
    private void markOutboxConfirmed(CorrelationData correlationData) {
        if (correlationData == null || correlationData.getId() == null
                || !correlationData.getId().startsWith(MqConstants.OUTBOX_CORRELATION_PREFIX)) {
            return;
        }
        try {
            outboxService.markConfirmed(Long.parseLong(
                    correlationData.getId().substring(MqConstants.OUTBOX_CORRELATION_PREFIX.length())));
        } catch (Exception exception) {
            log.error("发件箱确认标记失败，correlationId={}", correlationData.getId(), exception);
        }
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

    /** 声明帖子域交换机，持久化，topic 类型为将来按事件类别分流预留。 */
    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(MqConstants.POST_EXCHANGE, true, false);
    }

    /**
     * 声明帖子互动事件队列：与日志队列同构，durable 加死信绑定加 TTL 兜底，
     * 消费重试耗尽的消息经死信路由进入 DLQ 人工处理。
     */
    @Bean
    public Queue postInteractionQueue() {
        return QueueBuilder.durable(MqConstants.POST_INTERACTION_QUEUE)
                .deadLetterExchange(MqConstants.POST_INTERACTION_DLX)
                .deadLetterRoutingKey(MqConstants.POST_INTERACTION_DEAD_ROUTING)
                .ttl(7 * 24 * 60 * 60 * 1000)
                .build();
    }

    /** 将互动事件队列按互动路由键绑定到帖子域交换机。 */
    @Bean
    public Binding postInteractionBinding() {
        return BindingBuilder.bind(postInteractionQueue())
                .to(postExchange()).with(MqConstants.POST_INTERACTION_ROUTING);
    }

    /** 声明帖子互动死信交换机，持久化。 */
    @Bean
    public TopicExchange postInteractionDlxExchange() {
        return new TopicExchange(MqConstants.POST_INTERACTION_DLX, true, false);
    }

    /** 声明帖子互动死信队列，持久化，由人工消费处理异常互动消息。 */
    @Bean
    public Queue postInteractionDlqQueue() {
        return QueueBuilder.durable(MqConstants.POST_INTERACTION_DLQ).build();
    }

    /** 将帖子互动死信队列绑定到死信交换机。 */
    @Bean
    public Binding postInteractionDlqBinding() {
        return BindingBuilder.bind(postInteractionDlqQueue())
                .to(postInteractionDlxExchange()).with(MqConstants.POST_INTERACTION_DEAD_ROUTING);
    }
}
