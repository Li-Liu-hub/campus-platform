package com.campushub.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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

    /** 执行本次操作的用户主键，未登录操作允许为空。 */
    @TableField("log_user_id")
    private Long logUserId;

    /** 用户请求来源 IP 地址。 */
    @TableField("log_user_ip")
    private String logUserIp;

    /** 操作类型。 */
    @TableField("log_type")
    private String logType;

    /** 创建或修改的数据表主键，非表数据操作允许为空。 */
    @TableField("log_table_id")
    private Long logTableId;

    /** 操作状态：1 表示成功，0 表示失败。 */
    @TableField("log_status")
    private Integer logStatus;

    /** 失败原因或日志说明。 */
    @TableField("log_text")
    private String logText;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
