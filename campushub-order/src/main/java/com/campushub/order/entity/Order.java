package com.campushub.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单表实体。 */
@Data
@TableName("ch_order")
public class Order {

    /** 订单主键，使用雪花算法生成。 */
    @TableId(value = "order_id", type = IdType.ASSIGN_ID)
    private Long orderId;

    /** 下单用户主键。 */
    @TableField("order_user_id")
    private Long orderUserId;

    /** 订单标题。 */
    @TableField("order_title")
    private String orderTitle;

    /** 订单金额，保留两位小数。 */
    @TableField("order_amount")
    private BigDecimal orderAmount;

    /** 订单状态：0 待支付，1 已支付，2 已完成，3 已取消。 */
    @TableField("order_status")
    private Integer orderStatus;

    /** 软删除标识：0 未删除，1 已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("order_is_delete")
    private Integer orderIsDelete;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
