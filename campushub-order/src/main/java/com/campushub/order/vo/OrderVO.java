package com.campushub.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单接口返回对象。 */
public record OrderVO(
        Long orderId,
        Long orderSentUserId,
        Long orderReceiveUserId,
        String orderType,
        BigDecimal orderAmount,
        Integer orderStatus,
        LocalDateTime orderTimeout,
        Long orderViewNumber,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
