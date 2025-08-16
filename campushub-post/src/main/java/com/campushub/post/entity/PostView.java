package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子浏览明细表实体：每次浏览一行，聚合统计在 ch_post.post_view_number。 */
@Data
@TableName("ch_post_view")
public class PostView {

    /** 浏览明细主键，使用雪花算法生成。 */
    @TableId(value = "view_id", type = IdType.ASSIGN_ID)
    private Long viewId;

    /** 被浏览的帖子主键。 */
    @TableField("post_id")
    private Long postId;

    /** 浏览用户主键，当前需登录浏览，预留匿名浏览为空。 */
    @TableField("view_user_id")
    private Long viewUserId;

    /** 浏览时间由数据库维护，业务 SQL 不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
