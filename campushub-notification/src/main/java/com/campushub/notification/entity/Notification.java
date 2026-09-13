package com.campushub.notification.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知表实体。
 *
 * <p>通知是单向流水（谁触发、给谁看、什么内容），不做幂等去重——
 * 重复的点赞/关注天然产生多条通知，符合业务直觉；删除为物理删除（本人删自己的）。
 */
@Data
@TableName("ch_notification")
public class Notification {

    /** 通知主键，使用雪花算法生成。 */
    @TableId(value = "notification_id", type = IdType.ASSIGN_ID)
    private Long notificationId;

    /** 接收通知的用户 ID。 */
    @TableField("notification_user_id")
    private Long notificationUserId;

    /** 通知类型，取值见 NotificationTypes。 */
    @TableField("notification_type")
    private String notificationType;

    /** 通知内容。 */
    @TableField("notification_text")
    private String notificationText;

    /** 是否已读：0 未读，1 已读。 */
    @TableField("notification_is_read")
    private Integer notificationIsRead;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
