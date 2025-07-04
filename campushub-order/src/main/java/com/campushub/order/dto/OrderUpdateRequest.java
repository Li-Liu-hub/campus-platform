package com.campushub.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 修改订单请求参数，仅待接单状态可修改，字段留空表示不修改。 */
public record OrderUpdateRequest(
        @Size(max = 32, message = "订单类型长度不能超过 32 个字符")
        String orderType,

        @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "订单金额最多 8 位整数 2 位小数")
        BigDecimal orderAmount,

        @Future(message = "最晚支付时间必须晚于当前时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime orderTimeout
) {
}
