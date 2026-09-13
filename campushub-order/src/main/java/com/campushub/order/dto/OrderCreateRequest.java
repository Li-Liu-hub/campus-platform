package com.campushub.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 新增订单请求参数。 */
public record OrderCreateRequest(
        @NotBlank(message = "订单类型不能为空")
        @Size(max = 32, message = "订单类型长度不能超过 32 个字符")
        String orderType,

        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "订单金额最多 8 位整数 2 位小数")
        BigDecimal orderAmount,

        @NotNull(message = "最晚支付时间不能为空")
        @Future(message = "最晚支付时间必须晚于当前时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime orderTimeout,

        /** 图片地址列表（先经 /api/v1/files/upload 上传得到），最多 9 张，可为空 */
        @Size(max = 9, message = "单订单最多携带 9 张图片")
        List<@NotBlank(message = "图片地址不能为空")
             @Size(max = 512, message = "图片地址长度不能超过 512 个字符") String> imageUrls,

        @NotBlank(message = "订单幂等键不能为空")
        @Size(max = 64, message = "订单幂等键长度不能超过 64 个字符")
        String orderIdempotencyKey
) {
}
