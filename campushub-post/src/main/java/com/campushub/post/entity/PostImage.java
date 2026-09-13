package com.campushub.post.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子图片表实体。
 *
 * <p>不设独立幂等键：图片是帖子的子资源，重复提交由「帖子唯一键先拦、
 * 冲突时不插图片」的实现顺序兜住（与消息图片同一模式）。
 */
@Data
@TableName("ch_post_image")
public class PostImage {

    /** 帖子图片主键，使用雪花算法生成。 */
    @TableId(value = "post_image_id", type = IdType.ASSIGN_ID)
    private Long postImageId;

    /** 图片所属帖子 ID。 */
    @TableField("post_image_post_id")
    private Long postImagePostId;

    /** 图片地址。 */
    @TableField("post_image_url")
    private String postImageUrl;

    /** 同帖多图展示顺序，从 0 递增。 */
    @TableField("post_image_sort")
    private Integer postImageSort;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
