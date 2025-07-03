package com.campushub.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 新增订单请求参数。 */
public record OrderCreateRequest(
        @NotBlank(message = "订单标题不能为空")
        @Size(max = 128, message = "订单标题长度不能超过 128 个字符")
        String orderTitle,

        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "订单金额整数最多 8 位、最多 2 位小数")
        BigDecimal orderAmount
) {
}
