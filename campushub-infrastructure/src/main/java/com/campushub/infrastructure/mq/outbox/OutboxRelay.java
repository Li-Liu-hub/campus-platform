package com.campushub.infrastructure.mq.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.mq.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 功能：发件箱轮询中继，定时扫描未确认消息发送到 broker，并定期清理已确认记录。
 *
 * <p>可靠语义：status=0 的记录每轮都尝试发送，confirm 回执 ack=true 才标记为 1；
 * 发送失败或 confirm nack 都保持 0，下一轮自动重发——消息"至少一次"送达，
 * 重复投递由消费端幂等吸收。发送走 publishJson 的吞异常路径，MQ 故障只重试不报警刷屏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    /** 单轮最多处理的记录数，防止异常积压时单轮任务过长。 */
    private static final int BATCH_SIZE = 100;

    /** 已确认记录的保留天数，清理任务按此删除。 */
    private static final int RETAIN_DAYS = 7;

    private final OutboxMapper outboxMapper;

    private final MessagePublisher messagePublisher;

    /**
     * 功能：轮询发送待确认的发件箱消息。
     *
     * <p>消息携带 correlationId = "outbox:{id}"，confirm 回执 ack=true 时由回调标记已确认；
     * ack=false 或发送异常时不做任何标记，本条记录下轮继续重发。
     */
    @Scheduled(fixedDelayString = "${campushub.outbox.relay-interval-ms:5000}")
    public void relay() {
        List<OutboxMessage> pending = outboxMapper.selectList(new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getStatus, 0)
                .orderByAsc(OutboxMessage::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (OutboxMessage message : pending) {
            messagePublisher.publishJson(message.getExchange(), message.getRoutingKey(), message.getPayload(),
                    MqConstants.OUTBOX_CORRELATION_PREFIX + message.getId());
            outboxMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                    .setSql("retry_count = retry_count + 1")
                    .eq(OutboxMessage::getId, message.getId()));
        }
        if (!pending.isEmpty()) {
            log.info("发件箱本轮发送 {} 条待确认消息", pending.size());
        }
    }

    /**
     * 功能：每日清理已确认超过保留期的发件箱记录，控制表体积。
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanup() {
        int deleted = outboxMapper.delete(new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getStatus, 1)
                .lt(OutboxMessage::getConfirmTime, LocalDateTime.now().minusDays(RETAIN_DAYS)));
        if (deleted > 0) {
            log.info("发件箱清理 {} 条已确认超 {} 天的记录", deleted, RETAIN_DAYS);
        }
    }
}
