package com.campushub.infrastructure.mq.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** MQ 发件箱表实体，业务事务内写入的高价值消息记录。 */
@Data
@TableName("ch_outbox")
public class OutboxMessage {

    /** 主键，雪花算法生成，同时作为 confirm 关联的业务标识（前缀 outbox:）。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 目标交换机。 */
    @TableField("exchange")
    private String exchange;

    /** 路由键。 */
    @TableField("routing_key")
    private String routingKey;

    /** 业务标识，confirm 失败或人工排查时定位业务事件。 */
    @TableField("biz_id")
    private String bizId;

    /** 消息体 JSON 文本。 */
    @TableField("payload")
    private String payload;

    /** 发布状态：0 待确认（未确认即轮询重发），1 broker 已确认。 */
    @TableField("status")
    private Integer status;

    /** 轮询中继累计尝试发送次数。 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 创建时间，与业务数据同事务写入保证原子性。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** broker 确认时间，清理任务的判断依据。 */
    @TableField("confirm_time")
    private LocalDateTime confirmTime;
}
