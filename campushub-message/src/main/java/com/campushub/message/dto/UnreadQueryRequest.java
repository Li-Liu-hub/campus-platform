package com.campushub.message.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 未读消息拉取参数，游标分页：接收方上线后拉取全部未读消息。 */
public record UnreadQueryRequest(
        /** 上一页末行的消息 ID，首页不传 */
        Long cursorId,

        /** 页大小，缺省 20，最大 100 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
