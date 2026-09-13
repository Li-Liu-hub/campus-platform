package com.campushub.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 通知列表查询参数，游标分页：首页不传游标，翻页传上一页返回的通知 ID。 */
public record NotificationQueryRequest(
        /** 上一页末行的通知 ID，首页不传 */
        Long cursorId,

        /** 页大小，缺省 20，最大 100 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
