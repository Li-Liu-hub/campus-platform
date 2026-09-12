package com.campushub.message.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息表实体。
 *
 * <p>接收者冗余在消息行上：已读是「这条消息对接收者」的状态，独立成列后
 * 未读统计与标记已读都无需回查会话，也避免了将来会话语义变化时的推导歧义。
 */
@Data
@TableName("ch_message")
public class Message {

    /** 消息主键，使用雪花算法生成。 */
    @TableId(value = "message_id", type = IdType.ASSIGN_ID)
    private Long messageId;

    /** 该消息来自哪个会话。 */
    @TableField("message_conversation_id")
    private Long messageConversationId;

    /** 发送者用户 ID。 */
    @TableField("message_send_user_id")
    private Long messageSendUserId;

    /** 接收者用户 ID。 */
    @TableField("message_receive_user_id")
    private Long messageReceiveUserId;

    /** 消息内容，纯图片消息允许为空。 */
    @TableField("message_text")
    private String messageText;

    /** 是否已读：0 未读，1 已读。 */
    @TableField("message_is_read")
    private Integer messageIsRead;

    /** 发送消息幂等键，由前端每次提交时生成并传递。 */
    @TableField("message_idempotency_key")
    private String messageIdempotencyKey;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
