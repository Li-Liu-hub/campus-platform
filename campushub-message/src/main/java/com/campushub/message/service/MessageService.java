package com.campushub.message.service;

import com.campushub.message.dto.MessageQueryRequest;
import com.campushub.message.dto.MessageSendRequest;
import com.campushub.message.dto.UnreadQueryRequest;
import com.campushub.message.vo.MessagePageVO;
import com.campushub.message.vo.MessageVO;

/** 消息基础业务接口。 */
public interface MessageService {

    /**
     * 功能：发送消息，落库成功后向接收方实时推送（在线直推，离线等上线拉取）。
     *
     * <p>幂等由数据库唯一键保证：重复提交返回原消息且不重推、不重插图片。
     */
    MessageVO send(MessageSendRequest request);

    /** 查询会话内消息列表，按消息 ID 倒序游标分页，仅会话参与者可见。 */
    MessagePageVO listByConversation(MessageQueryRequest request);

    /** 拉取当前用户全部未读消息，按消息 ID 倒序游标分页（上线补偿路径）。 */
    MessagePageVO pullUnread(UnreadQueryRequest request);

    /**
     * 功能：把某会话中当前用户的未读消息标记为已读。
     *
     * @param conversationId 会话 ID，仅会话参与者可操作
     * @return 本次实际标记的消息条数，重复调用返回 0（幂等）
     */
    int markRead(Long conversationId);
}
