package com.campushub.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 日志表实体。 */
@Data
@TableName("ch_log")
public class Log {

    /** 日志主键，使用雪花算法生成。 */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    private Long logId;

    /** 事件唯一号（UUID）：消费端按它吸收 MQ 重复投递，取代原“类型+目标+时间”的秒级复合唯一键。 */
    @TableField("log_event_id")
    private String logEventId;

    /** 执行本次操作的用户主键，未登录操作允许为空。 */
    @TableField("log_user_id")
    private Long logUserId;

    /** 用户请求来源 IP 地址。 */
    @TableField("log_user_ip")
    private String logUserIp;

    /** 操作类型。 */
    @TableField("log_type")
    private String logType;

    /** 操作目标数据主键，非表数据操作允许为空。 */
    @TableField("log_target_id")
    private Long logTargetId;

    /** 操作状态：1 表示成功，0 表示失败。 */
    @TableField("log_status")
    private Integer logStatus;

    /** 失败原因或日志说明。 */
    @TableField("log_text")
    private String logText;

    /**
     * 创建时间：消费端路径写入事件携带的操作发生时刻，HTTP 管理端路径留空，
     * 由数据库 DEFAULT CURRENT_TIMESTAMP 填充。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 日志耗时（毫秒）。 */
    @TableField("log_cost_time")
    private Long logCostTime;
}
