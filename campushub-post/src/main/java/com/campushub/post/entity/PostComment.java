package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子评论表实体。
 *
 * <p>两级结构共存于一张表：{@code commentFatherId = 0} 为一级评论（直接评帖子），
 * 非 0 为二级回复（挂在其父一级评论下，对所有层级回复平铺，不嵌套三层）。
 * 被回复人冗余在 {@code commentReceiveUserId}：一级评论记录帖子作者，
 * 二级回复记录被回复评论的作者，列表组装"回复 @某人"时无需回查父评论。
 */
@Data
@TableName("ch_post_comment")
public class PostComment {

    /** 评论主键，使用雪花算法生成。 */
    @TableId(value = "comment_id", type = IdType.ASSIGN_ID)
    private Long commentId;

    /** 所属帖子 ID。 */
    @TableField("comment_post_id")
    private Long commentPostId;

    /** 评论者用户 ID。 */
    @TableField("comment_sent_user_id")
    private Long commentSentUserId;

    /** 父评论 ID：0 表示一级评论，否则为被回复的一级评论 ID。 */
    @TableField("comment_father_id")
    private Long commentFatherId;

    /** 被回复的用户 ID：一级评论为帖子作者，二级回复为被回复评论的作者。 */
    @TableField("comment_receive_user_id")
    private Long commentReceiveUserId;

    /** 评论内容。 */
    @TableField("comment_text")
    private String commentText;

    /** 评论幂等键，由前端每次提交时生成并传递。 */
    @TableField("comment_idempotency_key")
    private String commentIdempotencyKey;

    /** 软删除标识：0 未删除，1 已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("comment_is_delete")
    private Integer commentIsDelete;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
