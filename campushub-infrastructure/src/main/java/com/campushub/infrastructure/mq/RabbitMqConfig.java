package com.campushub.infrastructure.mq;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 配置：消息体统一使用 JSON 序列化。 */
@Configuration
public class RabbitMqConfig {

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
}
