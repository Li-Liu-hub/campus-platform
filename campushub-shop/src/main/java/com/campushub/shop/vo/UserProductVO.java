package com.campushub.shop.vo;

import java.time.LocalDateTime;

/** 用户购买商品记录接口返回对象。 */
public record UserProductVO(
        Long userProductId,
        Long userId,
        Long productId,
        Long productNumber,
        LocalDateTime purchaseTime,
        LocalDateTime paymentTime,
        Integer orderStatus,
        String userProductIdempotencyKey,
        LocalDateTime createTime
) {
}
