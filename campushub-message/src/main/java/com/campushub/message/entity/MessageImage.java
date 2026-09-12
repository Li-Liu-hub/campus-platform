package com.campushub.message.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息图片表实体。
 *
 * <p>刻意不设独立幂等键：图片是消息的子资源，重复提交由「消息唯一键先拦、
 * 冲突时不插图片」的实现顺序兜住，加第二个幂等键反而引入两把键错位的组合。
 */
@Data
@TableName("ch_message_image")
public class MessageImage {

    /** 消息图片主键，使用雪花算法生成。 */
    @TableId(value = "message_image_id", type = IdType.ASSIGN_ID)
    private Long messageImageId;

    /** 图片属于哪个消息。 */
    @TableField("message_image_message_id")
    private Long messageImageMessageId;

    /** 图片地址。 */
    @TableField("message_image_url")
    private String messageImageUrl;

    /** 同消息多图展示顺序，从 0 递增。 */
    @TableField("message_image_sort")
    private Integer messageImageSort;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
