package com.campushub.message.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息接口返回对象。
 *
 * <p>字段名与表列对齐（messageIsRead 而非 read/isRead）：Jackson 对布尔风格
 * 字段名有去前缀约定，显式命名避免序列化后的 JSON 键名歧义。
 */
public record MessageVO(
        Long messageId,
        Long conversationId,
        Long sendUserId,
        Long receiveUserId,
        String messageText,
        List<String> imageUrls,
        Integer messageIsRead,
        LocalDateTime createTime
) {
}
