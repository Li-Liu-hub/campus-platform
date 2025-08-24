package com.campushub.shop.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 店铺表实体。 */
@Data
@TableName("ch_shop")
public class Shop {

    /** 店铺主键，使用雪花算法生成。 */
    @TableId(value = "shop_id", type = IdType.ASSIGN_ID)
    private Long shopId;

    /** 店主用户 ID。 */
    @TableField("shop_user_id")
    private Long shopUserId;

    /** 店铺名称。 */
    @TableField("shop_name")
    private String shopName;

    /** 店铺简介。 */
    @TableField("shop_description")
    private String shopDescription;

    /** 店铺幂等键，由前端每次提交时生成并传递，唯一索引兜底防止重复创建。 */
    @TableField("shop_idempotency_key")
    private String shopIdempotencyKey;

    /** 软删除标识：0 表示未删除，1 表示已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("shop_is_delete")
    private Integer shopIsDelete;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
