package com.campushub.shop.constant;

/** 购买记录状态常量，对应 ch_user_product.order_status 的取值。 */
public final class PurchaseStatuses {

    /** 待付款：已下单并冻结库存，等待买家付款。 */
    public static final int PENDING_PAYMENT = 0;

    /** 已付款：已完成付款，冻结量已在付款时消耗。 */
    public static final int PAID = 1;

    /** 已取消：买家主动取消或超时任务关闭，冻结量已回补到可卖量。 */
    public static final int CANCELLED = 2;

    /** 常量类，禁止实例化。 */
    private PurchaseStatuses() {
    }
}
