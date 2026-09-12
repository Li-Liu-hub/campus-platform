package com.campushub.message.vo;

import java.util.List;

/**
 * 消息列表分页返回对象。
 *
 * <p>游标为消息 ID 单列：雪花 ID 单调递增，无需时间戳列参与定位。
 */
public record MessagePageVO(
        List<MessageVO> list,
        boolean hasMore,
        Long nextCursorId
) {
}
