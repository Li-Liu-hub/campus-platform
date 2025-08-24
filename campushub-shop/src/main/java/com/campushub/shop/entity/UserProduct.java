package com.campushub.shop.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户购买商品表实体，本表无软删列，删除为物理删除。 */
@Data
@TableName("ch_user_product")
public class UserProduct {

    /** 用户购买商品记录主键，使用雪花算法生成。 */
    @TableId(value = "user_product_id", type = IdType.ASSIGN_ID)
    private Long userProductId;

    /** 购买用户 ID。 */
    @TableField("user_id")
    private Long userId;

    /** 商品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 购买数量。 */
    @TableField("product_number")
    private Long productNumber;

    /** 下单时间。 */
    @TableField("purchase_time")
    private LocalDateTime purchaseTime;

    /** 预约付款时间，未预约时为空。 */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /** 订单状态：0 待付款，1 已付款，2 已取消。 */
    @TableField("order_status")
    private Integer orderStatus;

    /** 下单幂等键，由前端每次提交时生成并传递，唯一索引兜底防止重复下单重复扣库存。 */
    @TableField("user_product_idempotency_key")
    private String userProductIdempotencyKey;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
