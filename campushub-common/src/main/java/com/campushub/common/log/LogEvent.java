package com.campushub.common.log;

/**
 * 功能：操作日志事件契约，切面在业务执行后组装，经消息队列异步投递给消费端落库。
 *
 * <p>时间字段统一使用 epoch 毫秒，避免 Java 时间类型在 JSON 序列化中的时区与格式歧义。
 *
 * @param type        操作类型，见 OperationTypes 常量
 * @param targetId    操作目标数据的主键，无具体目标时为 null
 * @param userId      操作用户 ID，未登录操作为 null
 * @param ip          用户请求来源 IP，非 Web 上下文为 "unknown"
 * @param success     操作是否成功，true 成功 false 失败
 * @param text        失败原因截断后的文本，成功时为 null
 * @param costTime    业务执行耗时（毫秒）
 * @param operateTime 操作发生时刻（epoch 毫秒），消费端写入 create_time，
 *                    保证审计时间与操作时刻一致而非落库时刻
 */
public record LogEvent(
        String type,
        Long targetId,
        Long userId,
        String ip,
        boolean success,
        String text,
        Long costTime,
        Long operateTime
) {
}
