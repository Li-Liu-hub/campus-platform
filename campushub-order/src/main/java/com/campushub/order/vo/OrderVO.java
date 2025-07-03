package com.campushub.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单接口返回对象。 */
public record OrderVO(
        Long orderId,
        Long orderUserId,
        String orderTitle,
        BigDecimal orderAmount,
        Integer orderStatus,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
