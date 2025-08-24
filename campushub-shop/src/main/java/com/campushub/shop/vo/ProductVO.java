package com.campushub.shop.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品接口返回对象。 */
public record ProductVO(
        Long productId,
        Long shopId,
        String productName,
        BigDecimal productPrice,
        Long productStock,
        Integer productStatus,
        String productIdempotencyKey,
        LocalDateTime createTime
) {
}
