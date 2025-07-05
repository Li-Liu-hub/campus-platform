package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子收藏表实体，同一用户对同一帖子仅允许一条记录。 */
@Data
@TableName("ch_collect")
public class PostCollect {

    /** 收藏记录主键，使用雪花算法生成。 */
    @TableId(value = "collect_id", type = IdType.ASSIGN_ID)
    private Long collectId;

    /** 被收藏的帖子 ID。 */
    @TableField("collect_post_id")
    private Long collectPostId;

    /** 收藏用户 ID。 */
    @TableField("collect_user_id")
    private Long collectUserId;

    /** 创建时间由数据库维护，插入 SQL 不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
