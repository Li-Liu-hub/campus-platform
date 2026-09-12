package com.campushub.message.vo;

import java.time.LocalDateTime;

/**
 * 会话接口返回对象。
 *
 * <p>列表场景携带最后一条消息与未读数；详情场景两者可为空/零
 * （详情不需要回看消息内容，前端打开会话后走消息列表接口）。
 */
public record ConversationVO(
        Long conversationId,
        Long peerUserId,
        String peerNickname,
        String conversationTitle,
        String lastMessageText,
        LocalDateTime lastMessageTime,
        Long unreadCount,
        LocalDateTime createTime
) {
}
