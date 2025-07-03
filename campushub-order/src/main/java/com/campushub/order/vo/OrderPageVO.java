package com.campushub.order.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 订单游标分页返回对象：hasMore 为 true 时携带下一页游标二元组。 */
public record OrderPageVO(
        List<OrderVO> list,
        boolean hasMore,
        LocalDateTime nextCursorTime,
        Long nextCursorId
) {
}
