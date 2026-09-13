package com.campushub.notification.vo;

import java.util.List;

/**
 * 通知列表分页返回对象。
 *
 * <p>游标为通知 ID 单列：雪花 ID 单调递增，无需时间戳列参与定位。
 */
public record PageVO<T>(
        List<T> list,
        boolean hasMore,
        Long nextCursorId
) {
}
