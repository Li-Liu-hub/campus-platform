package com.campushub.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** 订单条件查询请求参数，查询当前登录用户发布或接收的订单。 */
public record OrderQueryRequest(
        @Pattern(regexp = "sent|received", message = "查询角色只能为 sent 或 received")
        /** 查询角色：sent 我发布的，received 我接的，缺省 sent。 */
        String role,

        /** 订单状态过滤，取值见 OrderStatuses，缺省不过滤。 */
        @Min(value = 0, message = "订单状态取值不合法")
        @Max(value = 3, message = "订单状态取值不合法")
        Integer orderStatus,

        /** 页大小，缺省 20，最大 100。 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
