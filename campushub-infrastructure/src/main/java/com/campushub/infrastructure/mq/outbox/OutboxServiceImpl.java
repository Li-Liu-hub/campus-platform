package com.campushub.infrastructure.mq.outbox;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** MQ 发件箱写入实现。 */
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxMapper outboxMapper;

    private final ObjectMapper objectMapper;

    /**
     * 功能：向发件箱写入一条待发布消息，仅落库不发送。
     *
     * <p>调用方处于自身事务内时，本插入与业务数据同事务提交；事务回滚则消息记录一并消失，
     * 这正是 outbox 保证"业务成功则消息必达"的核心。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键
     * @param bizId 业务标识，允许为 null
     * @param payload 消息内容，任意可 JSON 序列化的对象
     * @throws Exception 序列化或落库失败时抛出，由业务事务整体回滚
     */
    @Override
    public void append(String exchange, String routingKey, String bizId, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setExchange(exchange);
        message.setRoutingKey(routingKey);
        message.setBizId(bizId);
        try {
            message.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception exception) {
            // 序列化失败说明消息体不可 JSON 化，属编程错误，抛出让业务事务回滚而非静默丢消息
            throw new IllegalArgumentException("发件箱消息序列化失败，exchange=" + exchange, exception);
        }
        message.setStatus(0);
        outboxMapper.insert(message);
    }

    /**
     * 功能：将发件箱记录标记为 broker 已确认。
     *
     * <p>条件更新只允许 0 → 1 的单向流转，confirm 回执与轮询中继并发时不会重复标记。
     *
     * @param id 发件箱记录主键
     * @return 标记成功返回 true；记录不存在或已确认返回 false
     */
    @Override
    public boolean markConfirmed(Long id) {
        return outboxMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .set(OutboxMessage::getStatus, 1)
                .set(OutboxMessage::getConfirmTime, LocalDateTime.now())
                .eq(OutboxMessage::getId, id)
                .eq(OutboxMessage::getStatus, 0)) > 0;
    }
}
