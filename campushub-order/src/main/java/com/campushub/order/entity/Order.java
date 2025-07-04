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

    /** 发布订单的用户 ID。 */
    @TableField("order_sent_user_id")
    private Long orderSentUserId;

    /** 接单用户 ID，抢单成功后回填，待接单时为空。 */
    @TableField("order_receive_user_id")
    private Long orderReceiveUserId;

    /** 防止同一请求重复创建订单的幂等键。 */
    @TableField("order_idempotency_key")
    private String orderIdempotencyKey;

    /** 订单金额，保留两位小数。 */
    @TableField("order_amount")
    private BigDecimal orderAmount;

    /** 订单状态，取值见 OrderStatuses 常量。 */
    @TableField("order_status")
    private Integer orderStatus;

    /** 订单最晚支付时间，超时未支付可被取消。 */
    @TableField("order_timeout")
    private LocalDateTime orderTimeout;

    /** 订单类型。 */
    @TableField("order_type")
    private String orderType;

    /** 订单浏览量。 */
    @TableField("order_view_number")
    private Long orderViewNumber;

    /** 软删除标识：0 表示未删除，1 表示已删除。 */
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
