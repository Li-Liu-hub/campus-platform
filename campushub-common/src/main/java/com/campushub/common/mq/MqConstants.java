package com.campushub.common.mq;

/**
 * RabbitMQ 命名常量集中地：交换机、队列、路由键的全部名字只在此处定义，
 * 生产者与消费者统一引用，裸字符串消失使 typo 从源头消灭。
 *
 * <p>命名体系即防御：全小写、点分段、项目前缀，topic 类型的分段结构
 * （项目.域.对象.动作）为将来按类别分流预留了口子。
 */
public final class MqConstants {

    /** 操作日志交换机（topic），接收 campushub.log.operation 等日志域事件。 */
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

    /** 缓存域交换机（topic），延迟双删链路预留，功能落地时启用。 */
    public static final String CACHE_EXCHANGE = "campushub.cache";

    /** 缓存删除队列，延迟双删链路预留，功能落地时启用。 */
    public static final String CACHE_DELETE_QUEUE = "campushub.cache.delete.queue";

    /** 延迟双删的路由键，延迟双删链路预留，功能落地时启用。 */
    public static final String CACHE_DELAY_DELETE_ROUTING = "campushub.cache.delay-delete";

    /** 缓存删除的死信改写键，延迟双删链路预留，功能落地时启用。 */
    public static final String CACHE_DELETE_ROUTING = "campushub.cache.delete";

    /** 发件箱消息的 correlationId 前缀，confirm 回调凭该前缀识别 outbox 消息并标记已确认。 */
    public static final String OUTBOX_CORRELATION_PREFIX = "outbox:";

    /** 私有构造，防止实例化常量类。 */
    private MqConstants() {
    }
}
