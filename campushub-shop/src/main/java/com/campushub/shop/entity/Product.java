package com.campushub.shop.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品表实体。 */
@Data
@TableName("ch_product")
public class Product {

    /** 商品主键，使用雪花算法生成。 */
    @TableId(value = "product_id", type = IdType.ASSIGN_ID)
    private Long productId;

    /** 所属店铺 ID。 */
    @TableField("product_shop_id")
    private Long productShopId;

    /** 商品名称。 */
    @TableField("product_name")
    private String productName;

    /** 商品价格，保留两位小数。 */
    @TableField("product_price")
    private BigDecimal productPrice;

    /** 可卖库存：下单冻结时减少，取消或超时后回补，付款后不再回补。 */
    @TableField("product_stock")
    private Long productStock;

    /** 冻结库存：已下单未付款占用中，付款后消耗。卖家不可手工调整。 */
    @TableField("product_stock_locked")
    private Long productStockLocked;

    /** 上架状态：1 上架，0 下架。 */
    @TableField("product_status")
    private Integer productStatus;

    /** 商品幂等键，由前端每次提交时生成并传递，唯一索引兜底防止重复创建。 */
    @TableField("product_idempotency_key")
    private String productIdempotencyKey;

    /** 软删除标识：0 表示未删除，1 表示已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("product_is_delete")
    private Integer productIsDelete;

    /** 创建时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间由数据库维护，新增和更新 SQL 均不写入该字段。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
