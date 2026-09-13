package com.campushub.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单图片表实体。
 *
 * <p>不设独立幂等键：图片是订单的子资源，重复提交由「订单唯一键先拦、
 * 冲突时不插图片」的实现顺序兜住（与消息/帖子图片同一模式）。
 */
@Data
@TableName("ch_order_image")
public class OrderImage {

    /** 订单图片主键，使用雪花算法生成。 */
    @TableId(value = "order_image_id", type = IdType.ASSIGN_ID)
    private Long orderImageId;

    /** 图片所属订单 ID。 */
    @TableField("order_image_order_id")
    private Long orderImageOrderId;

    /** 图片地址。 */
    @TableField("order_image_url")
    private String orderImageUrl;

    /** 同单多图展示顺序，从 0 递增。 */
    @TableField("order_image_sort")
    private Integer orderImageSort;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
