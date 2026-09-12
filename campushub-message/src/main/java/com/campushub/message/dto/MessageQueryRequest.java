package com.campushub.message.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 会话内消息列表查询参数。
 *
 * <p>游标用消息 ID 单列而非 (时间, ID) 二元组：消息 ID 为雪花算法生成且单调递增，
 * 单列游标即可精确定位且直接命中索引，无需第二列兜底同时间戳。
 */
public record MessageQueryRequest(
        @NotNull(message = "会话 ID 不能为空")
        Long conversationId,

        /** 上一页末行的消息 ID，首页不传 */
        Long cursorId,

        /** 页大小，缺省 20，最大 100 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
