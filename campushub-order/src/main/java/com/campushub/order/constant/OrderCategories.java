package com.campushub.order.constant;

import java.util.Set;

/** 订单允许使用的类型。 */
public final class OrderCategories {

    /** 系统支持的订单类型名称。 */
    public static final Set<String> VALUES = Set.of(
            "跑腿代取",
            "代买代购",
            "其他互助"
    );

    /** 常量类，禁止实例化。 */
    private OrderCategories() {
    }
}
