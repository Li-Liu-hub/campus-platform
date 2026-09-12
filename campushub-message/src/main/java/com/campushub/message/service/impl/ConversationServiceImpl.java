package com.campushub.message.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.message.dto.ConversationCreateRequest;
import com.campushub.message.dto.ConversationQueryRequest;
import com.campushub.message.entity.Conversation;
import com.campushub.message.mapper.ConversationListRow;
import com.campushub.message.mapper.ConversationMapper;
import com.campushub.message.service.ConversationService;
import com.campushub.message.vo.ConversationVO;
import com.campushub.message.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 会话基础业务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

    private final AuthenticationService authenticationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 功能：获取或创建两人会话，重复提交收敛到原会话。
     *
     * <p>插入前把参与双方规范化为「小 ID 在前、大 ID 在后」，唯一键 uk_conversation_users
     * 保证一对用户至多一个会话——这比幂等键更强：换幂等键重试、对方反向发起同样被拦。
     * 冲突回查分两步：先按参与者双列回查（业务语义的重复），再按幂等键回查（同一请求重放），
     * 两者都没有说明幂等键被别的会话占用，抛 409。
     *
     * @param request 创建请求，含目标用户 ID 与前端生成的幂等键
     * @return 会话信息；已存在时返回原会话
     * @throws BusinessException 与自己建会话抛 400，目标用户不存在抛 404，
     *                          幂等键被他人会话占用抛 409，登录失效抛 401
     */
    @Override
    @Transactional
    public ConversationVO createOrGet(ConversationCreateRequest request) {
        Long userId = requireCurrentUserId();
        Long targetUserId = request.targetUserId();
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能与自己创建会话");
        }
        String peerNickname = conversationMapper.selectActiveUserNickname(targetUserId);
        if (peerNickname == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }
        Long userAId = Math.min(userId, targetUserId);
        Long userBId = Math.max(userId, targetUserId);

        Conversation conversation = new Conversation();
        conversation.setConversationUserAId(userAId);
        conversation.setConversationUserBId(userBId);
        conversation.setConversationIdempotencyKey(request.conversationIdempotencyKey().trim());
        try {
            conversationMapper.insert(conversation);
            return new ConversationVO(conversation.getConversationId(), targetUserId, peerNickname,
                    conversation.getConversationTitle(), null, null, 0L, conversation.getCreateTime());
        } catch (DuplicateKeyException exception) {
            Conversation existing = findByUsers(userAId, userBId);
            if (existing == null) {
                existing = findByIdempotencyKey(conversation.getConversationIdempotencyKey());
            }
            // 幂等键回查到的会话必须包含当前用户，否则是他人会话占用了该键，不得泄露其内容
            if (existing == null || !isParticipant(existing, userId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "会话幂等键已被使用");
            }
            return toConversationVO(existing, userId);
        }
    }

    /** 查询当前用户参与的会话列表，一条聚合 SQL 携带对方信息、最后消息与未读数。 */
    @Override
    public PageVO<ConversationVO> query(ConversationQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        List<ConversationListRow> rows = conversationMapper.selectPageWithPeer(
                userId, request.cursorTime(), request.cursorId(), pageSize);
        List<ConversationVO> list = rows.stream().map(this::toConversationVO).toList();
        if (rows.size() < pageSize) {
            return new PageVO<>(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        ConversationListRow lastRow = rows.get(rows.size() - 1);
        return new PageVO<>(list, true, lastRow.getCreateTime(), lastRow.getConversationId());
    }

    /** 查询会话详情，仅参与者可见；对方昵称实时查询，对方已注销时为 null。 */
    @Override
    public ConversationVO getById(Long conversationId) {
        Long userId = requireCurrentUserId();
        Conversation conversation = requireParticipant(conversationId, userId);
        return toConversationVO(conversation, userId);
    }

    /** 按参与者双列查询会话，未建会话时返回 null。 */
    private Conversation findByUsers(Long userAId, Long userBId) {
        return conversationMapper.selectOne(Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getConversationUserAId, userAId)
                .eq(Conversation::getConversationUserBId, userBId)
                .last("LIMIT 1"));
    }

    /** 按幂等键查询会话，未命中时返回 null。 */
    private Conversation findByIdempotencyKey(String idempotencyKey) {
        return conversationMapper.selectOne(Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getConversationIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    /** 校验会话存在且当前用户是参与者，否则抛出对应业务异常。 */
    private Conversation requireParticipant(Long conversationId, Long userId) {
        if (conversationId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话 ID 不能为空");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!isParticipant(conversation, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    /** 判断用户是否为会话参与者。 */
    private boolean isParticipant(Conversation conversation, Long userId) {
        return userId.equals(conversation.getConversationUserAId())
                || userId.equals(conversation.getConversationUserBId());
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 将会话实体转换为返回对象，对方 ID 与昵称实时计算。 */
    private ConversationVO toConversationVO(Conversation conversation, Long userId) {
        Long peerUserId = peerOf(conversation, userId);
        return new ConversationVO(
                conversation.getConversationId(),
                peerUserId,
                conversationMapper.selectActiveUserNickname(peerUserId),
                conversation.getConversationTitle(),
                null,
                null,
                0L,
                conversation.getCreateTime()
        );
    }

    /** 将列表查询行转换为返回对象。 */
    private ConversationVO toConversationVO(ConversationListRow row) {
        return new ConversationVO(
                row.getConversationId(),
                row.getPeerUserId(),
                row.getPeerNickname(),
                row.getConversationTitle(),
                row.getLastMessageText(),
                row.getLastMessageTime(),
                row.getUnreadCount() == null ? 0L : row.getUnreadCount(),
                row.getCreateTime()
        );
    }

    /** 取会话中当前用户之外的另一个参与者。 */
    private Long peerOf(Conversation conversation, Long userId) {
        return userId.equals(conversation.getConversationUserAId())
                ? conversation.getConversationUserBId()
                : conversation.getConversationUserAId();
    }
}
