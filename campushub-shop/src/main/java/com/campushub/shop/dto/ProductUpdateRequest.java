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

        /**
         * 库存调整量：正数表示补货增加，负数表示减少，0 表示本次不调整库存。
         *
         * <p>刻意不接收库存绝对值：商家编辑页展示的库存是读取时的快照，若按绝对值整体覆盖，
         * 会把快照之后并发下单已经扣掉的库存又写回去，导致可售数量大于实际库存（超卖）。
         * 调整量不依赖读取到的旧值，交给数据库做增减运算即可避免该问题。
         */
        @NotNull(message = "库存调整量不能为空，不调整时传 0")
        Long productStockDelta,

        @NotNull(message = "上架状态不能为空")
        @Min(value = 0, message = "上架状态只能为 0 或 1")
        @Max(value = 1, message = "上架状态只能为 0 或 1")
        Integer productStatus
) {
}
