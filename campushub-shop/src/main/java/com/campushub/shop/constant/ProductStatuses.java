package com.campushub.shop.constant;

/** 商品上架状态常量，对应 ch_product.product_status 的取值。 */
public final class ProductStatuses {

    /** 未上架：卖家主动撤下，与库存数量无关。 */
    public static final int OFF_SHELF = 0;

    /** 已上架：在售中，是否买得到取决于可卖库存。 */
    public static final int ON_SHELF = 1;

    /** 常量类，禁止实例化。 */
    private ProductStatuses() {
    }
}
