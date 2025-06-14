package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子表实体。 */
@Data
@TableName("ch_post")
public class Post {

    /** 帖子主键，使用雪花算法生成。 */
    @TableId(value = "post_id", type = IdType.ASSIGN_ID)
    private Long postId;

    /** 发帖用户主键。 */
    @TableField("post_user_id")
    private Long postUserId;

    /** 帖子标题。 */
    @TableField("post_title")
    private String postTitle;

    /** 帖子类型。 */
    @TableField("post_type")
    private String postType;

    /** 帖子正文内容。 */
    @TableField("post_text")
    private String postText;

    /** 防止同一请求重复创建帖子的幂等键。 */
    @TableField("post_idempotency_key")
    private String postIdempotencyKey;

    /** 帖子浏览量。 */
    @TableField("post_view_number")
    private Long postViewNumber;

    /** 作者昵称，来自 ch_user 关联查询，非本表字段。 */
    @TableField(exist = false)
    private String userNickname;

    /** 软删除标识：0 表示未删除，1 表示已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("post_is_delete")
    private Integer postIsDelete;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
