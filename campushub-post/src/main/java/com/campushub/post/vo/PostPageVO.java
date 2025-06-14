package com.campushub.post.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 帖子游标分页返回对象：hasMore 为 true 时携带下一页游标二元组。 */
public record PostPageVO(
        List<PostVO> list,
        boolean hasMore,
        LocalDateTime nextCursorTime,
        Long nextCursorId
) {
}
