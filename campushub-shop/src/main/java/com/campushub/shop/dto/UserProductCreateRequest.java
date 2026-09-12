package com.campushub.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 下单（新增用户购买商品记录）请求参数。 */
public record UserProductCreateRequest(
        @NotNull(message = "商品 ID 不能为空")
        Long productId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量不能小于 1")
        Long productNumber,

        @NotBlank(message = "下单幂等键不能为空")
        @Size(max = 64, message = "下单幂等键长度不能超过 64 个字符")
        String userProductIdempotencyKey
) {
}
