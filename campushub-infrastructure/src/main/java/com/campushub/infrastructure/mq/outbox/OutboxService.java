package com.campushub.infrastructure.mq.outbox;

/** MQ 发件箱写入能力接口，供高价值消息（丢失即资损，如订单事件）的业务方在事务内调用。 */
public interface OutboxService {

    /**
     * 功能：向发件箱写入一条待发布消息，业务方在自身事务内调用，与本库业务数据同事务提交。
     *
     * <p>消息的实际发送由轮询中继完成，本方法只保证"业务数据与消息记录"的原子性。
     *
     * @param exchange 目标交换机名称
     * @param routingKey 路由键
     * @param bizId 业务标识，confirm 失败或人工排查时定位业务事件，允许为 null
     * @param payload 消息内容，任意可 JSON 序列化的对象
     * @throws Exception 序列化或落库失败时抛出，由业务事务整体回滚
     */
    void append(String exchange, String routingKey, String bizId, Object payload);

    /**
     * 功能：将发件箱记录标记为 broker 已确认，供 confirm 回执 ack=true 时调用。
     *
     * @param id 发件箱记录主键
     * @return 标记成功返回 true；记录不存在或已确认返回 false
     */
    boolean markConfirmed(Long id);
}
