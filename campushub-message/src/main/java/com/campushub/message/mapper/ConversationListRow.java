package com.campushub.message.mapper;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表查询行：会话基本信息 + 对方信息 + 最后一条消息 + 未读数，
 * 由一条聚合 SQL 一次组装，避免每会话回查（N+1）。
 *
 * <p>非表结构载体，仅用于承接 {@link ConversationMapper#selectPageWithPeer} 的查询结果。
 */
@Data
public class ConversationListRow {

    /** 会话 ID。 */
    private Long conversationId;

    /** 对方用户 ID（当前用户之外的参与者）。 */
    private Long peerUserId;

    /** 对方昵称。 */
    private String peerNickname;

    /** 会话标题。 */
    private String conversationTitle;

    /** 最后一条消息内容，空会话时为 null。 */
    private String lastMessageText;

    /** 最后一条消息时间，空会话时为 null。 */
    private LocalDateTime lastMessageTime;

    /** 当前用户在该会话中的未读消息数。 */
    private Long unreadCount;

    /** 会话创建时间，游标分页依据。 */
    private LocalDateTime createTime;
}
