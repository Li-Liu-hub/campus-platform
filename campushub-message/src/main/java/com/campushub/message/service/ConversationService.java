package com.campushub.message.service;

import com.campushub.message.dto.ConversationCreateRequest;
import com.campushub.message.dto.ConversationQueryRequest;
import com.campushub.message.vo.ConversationVO;
import com.campushub.message.vo.PageVO;

/** 会话基础业务接口。 */
public interface ConversationService {

    /**
     * 功能：获取或创建当前登录用户与目标用户的两人会话。
     *
     * <p>一对用户之间至多一个会话：参与双列规范化（小 ID 在前）加联合唯一键，
     * 任何方向、任何幂等键的重复提交都收敛到原会话。
     */
    ConversationVO createOrGet(ConversationCreateRequest request);

    /** 查询当前登录用户参与的会话列表，按创建时间倒序游标分页，携带最后消息与未读数。 */
    PageVO<ConversationVO> query(ConversationQueryRequest request);

    /** 查询会话详情，仅会话参与者可见。 */
    ConversationVO getById(Long conversationId);
}
