package com.campushub.shop.vo;

import com.campushub.shop.constant.ProductDisplayState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品接口返回对象。
 *
 * <p>库存刻意返回两个原始数字（可卖量、冻结量）而不是一个"剩余数量"：多件商品
 * "3 件可卖 + 1 件冻结"这种状态用单一数字表达不了，前端要展示"可卖 3 件"还是
 * "可卖 3 件（1 件交易中）"由自己决定。展示态枚举是给前端的统一判断依据，
 * 由可卖量、冻结量与上架状态推导得出，不落库。
 */
public record ProductVO(
        Long productId,
        Long shopId,
        String productName,
        BigDecimal productPrice,
        Long productStock,
        Long productStockLocked,
        Integer productStatus,
        ProductDisplayState productDisplayState,
        String productIdempotencyKey,
        LocalDateTime createTime
) {
}
