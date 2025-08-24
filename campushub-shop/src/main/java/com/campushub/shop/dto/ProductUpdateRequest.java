package com.campushub.shop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 修改商品请求参数。 */
public record ProductUpdateRequest(
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称长度不能超过 128 个字符")
        String productName,

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
        BigDecimal productPrice,

        @NotNull(message = "商品库存不能为空")
        @Min(value = 0, message = "商品库存不能小于 0")
        Long productStock,

        @NotNull(message = "上架状态不能为空")
        @Min(value = 0, message = "上架状态只能为 0 或 1")
        @Max(value = 1, message = "上架状态只能为 0 或 1")
        Integer productStatus
) {
}
