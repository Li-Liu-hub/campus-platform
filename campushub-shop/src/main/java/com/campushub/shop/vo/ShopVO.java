package com.campushub.shop.vo;

import java.time.LocalDateTime;

/** 店铺接口返回对象。 */
public record ShopVO(
        Long shopId,
        Long shopUserId,
        String shopName,
        String shopDescription,
        String shopIdempotencyKey,
        LocalDateTime createTime
) {
}
