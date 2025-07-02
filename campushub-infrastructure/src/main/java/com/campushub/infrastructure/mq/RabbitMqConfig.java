package com.campushub.infrastructure.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 配置：消息体统一使用 JSON 序列化，并声明操作日志的交换机、队列与死信兜底拓扑。 */
@Configuration
public class RabbitMqConfig {

    /** 操作日志交换机。 */
    public static final String LOG_EXCHANGE = "campushub.log";

    /** 操作日志路由键。 */
    public static final String LOG_ROUTING_KEY = "campushub.log.operation";

    /** 操作日志队列，持久化，消息 TTL 兜底。 */
    public static final String LOG_QUEUE = "campushub.log.queue";

    /** 操作日志死信交换机，接收超时或被拒绝的消息。 */
    public static final String LOG_DLX = "campushub.log.dlq";

    /** 操作日志死信路由键。 */
    public static final String LOG_DEAD_ROUTING = "campushub.log.dead";

    /** 操作日志死信队列，积压消息人工处理。 */
    public static final String LOG_DLQ = "campushub.log.dlq.queue";

    /** 日志消息 TTL 兜底：7 天未消费转入死信队列，防止队列无限堆积。 */
    private static final int LOG_MESSAGE_TTL_MS = 7 * 24 * 60 * 60 * 1000;

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

    /** 声明操作日志交换机，持久化，按路由键精确匹配。 */
    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(LOG_EXCHANGE, true, false);
    }

    /**
     * 声明操作日志队列：durable 保证 broker 重启消息不丢；
     * 绑定死信交换机与 TTL，超时未消费或消费拒绝的消息统一进入死信队列兜底。
     */
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(LOG_QUEUE)
                .deadLetterExchange(LOG_DLX)
                .deadLetterRoutingKey(LOG_DEAD_ROUTING)
                .ttl(LOG_MESSAGE_TTL_MS)
                .build();
    }

    /** 将日志队列按操作路由键绑定到日志交换机。 */
    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue()).to(logExchange()).with(LOG_ROUTING_KEY);
    }

    /** 声明死信交换机，持久化。 */
    @Bean
    public DirectExchange logDlxExchange() {
        return new DirectExchange(LOG_DLX, true, false);
    }

    /** 声明死信队列，持久化，由人工消费处理异常日志消息。 */
    @Bean
    public Queue logDlqQueue() {
        return QueueBuilder.durable(LOG_DLQ).build();
    }

    /** 将死信队列绑定到死信交换机。 */
    @Bean
    public Binding logDlqBinding() {
        return BindingBuilder.bind(logDlqQueue()).to(logDlxExchange()).with(LOG_DEAD_ROUTING);
    }
}
