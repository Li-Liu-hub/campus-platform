package com.campushub.message.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话表实体。
 *
 * <p>两人会话的身份由规范化双列表达：A 恒为双方用户 ID 的较小值、B 恒为较大值，
 * 联合唯一键保证任意方向、任意幂等键的重复创建都收敛到同一个会话。
 */
@Data
@TableName("ch_conversation")
public class Conversation {

    /** 会话主键，使用雪花算法生成。 */
    @TableId(value = "conversation_id", type = IdType.ASSIGN_ID)
    private Long conversationId;

    /** 参与者 A：恒取双方用户 ID 的较小值。 */
    @TableField("conversation_user_a_id")
    private Long conversationUserAId;

    /** 参与者 B：恒取双方用户 ID 的较大值。 */
    @TableField("conversation_user_b_id")
    private Long conversationUserBId;

    /** 会话标题，两人会话可空。 */
    @TableField("conversation_title")
    private String conversationTitle;

    /** 建会话幂等键，由前端每次提交时生成并传递。 */
    @TableField("conversation_idempotency_key")
    private String conversationIdempotencyKey;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
