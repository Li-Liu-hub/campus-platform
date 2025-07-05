package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子点赞表实体，同一用户对同一帖子仅允许一条记录。 */
@Data
@TableName("ch_like")
public class PostLike {

    /** 点赞记录主键，使用雪花算法生成。 */
    @TableId(value = "like_id", type = IdType.ASSIGN_ID)
    private Long likeId;

    /** 被点赞的帖子 ID。 */
    @TableField("like_post_id")
    private Long likePostId;

    /** 点赞用户 ID。 */
    @TableField("like_user_id")
    private Long likeUserId;

    /** 创建时间由数据库维护，插入 SQL 不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
