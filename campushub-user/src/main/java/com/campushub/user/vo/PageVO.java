package com.campushub.user.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 游标分页通用返回对象：hasMore 为 true 时携带下一页游标二元组。 */
public record PageVO<T>(
        List<T> list,
        boolean hasMore,
        LocalDateTime nextCursorTime,
        Long nextCursorId
) {
}
