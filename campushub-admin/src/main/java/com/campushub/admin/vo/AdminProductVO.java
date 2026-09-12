package com.campushub.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理端商品列表行，含所属店铺名称（SQL 直查组装，不依赖店铺模块代码）。 */
@Data
public class AdminProductVO {

    /** 商品 ID。 */
    private Long productId;

    /** 商品名称。 */
    private String productName;

    /** 所属店铺 ID。 */
    private Long productShopId;

    /** 所属店铺名称，店铺已注销时为空。 */
    private String shopName;

    /** 商品价格。 */
    private BigDecimal productPrice;

    /** 可卖库存。 */
    private Long productStock;

    /** 冻结库存。 */
    private Long productStockLocked;

    /** 上架状态：1 上架，0 下架。 */
    private Integer productStatus;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
