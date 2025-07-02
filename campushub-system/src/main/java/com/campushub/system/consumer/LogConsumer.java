package com.campushub.system.consumer;

import com.campushub.common.log.LogEvent;
import com.campushub.infrastructure.mq.RabbitMqConfig;
import com.campushub.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 功能：操作日志消费者，从日志队列取出事件写入 ch_log 表。
 *
 * <p>运行在 Spring AMQP 容器线程池上，AUTO 确认模式：方法正常返回自动 ACK，
 * 抛出异常由框架本地重试，重试耗尽后拒绝且不重回队列，经死信路由进入 DLQ 人工处理。
 */
@Component
@RequiredArgsConstructor
public class LogConsumer {

    private final LogService logService;

    /**
     * 功能：消费操作日志事件并落库。
     *
     * @param event 操作日志事件，容器按 JSON 反序列化
     * @throws Exception 落库失败时抛出，触发容器本地重试与死信兜底
     */
    @RabbitListener(queues = RabbitMqConfig.LOG_QUEUE)
    public void onLogEvent(LogEvent event) {
        logService.insert(event);
    }
}
