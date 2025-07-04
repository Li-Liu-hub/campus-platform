package com.campushub.order.constant;

/** 订单状态常量，对应 ch_order.order_status 的取值。 */
public final class OrderStatuses {

    /** 待接单：订单已发布，等待用户抢单。 */
    public static final int PENDING = 0;

    /** 已接单：已被用户抢到，等待支付。 */
    public static final int ACCEPTED = 1;

    /** 已完成：订单支付并履约结束。 */
    public static final int COMPLETED = 2;

    /** 已取消：订单主动取消或超时关闭。 */
    public static final int CANCELLED = 3;

    /** 常量类，禁止实例化。 */
    private OrderStatuses() {
    }
}
