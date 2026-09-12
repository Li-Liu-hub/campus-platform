package com.campushub.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 卖家查看某商品待付款记录的查询参数，游标分页：首页不传游标，翻页传上一页返回的游标二元组。 */
public record PendingPurchaseQueryRequest(
        /** 上一页末行的创建时间，首页不传 */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime cursorTime,

        /** 上一页末行的记录 ID，与 cursorTime 成对使用 */
        Long cursorId,

        /** 页大小，缺省 20，最大 100 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
