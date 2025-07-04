package com.campushub.order.vo;

import java.util.List;

/** 订单分页查询返回对象。 */
public record OrderPageVO(
        List<OrderVO> orders
) {
}
