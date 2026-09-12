package com.campushub.message.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.util.StringUtils;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.infrastructure.websocket.RealtimePushService;
import com.campushub.message.dto.MessageQueryRequest;
import com.campushub.message.dto.MessageSendRequest;
import com.campushub.message.dto.UnreadQueryRequest;
import com.campushub.message.entity.Conversation;
import com.campushub.message.entity.Message;
import com.campushub.message.entity.MessageImage;
import com.campushub.message.mapper.ConversationMapper;
import com.campushub.message.mapper.MessageImageMapper;
import com.campushub.message.mapper.MessageMapper;
import com.campushub.message.service.MessageService;
import com.campushub.message.vo.MessagePageVO;
import com.campushub.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息基础业务实现。
 *
 * <p>发送链路：Redis 不参与，MySQL 是唯一事实源；实时性由「落库成功后向在线接收方
 * 推送」保证，可靠性由「离线落库 + 上线拉取」保证——推送是尽力送达的加速器，
 * 推送失败不影响消息本身（已落库，可拉取）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    private final MessageImageMapper messageImageMapper;

    private final ConversationMapper conversationMapper;

    private final AuthenticationService authenticationService;

    private final RealtimePushService realtimePushService;

    private final StringUtils stringUtils;

    /** 未读标识：0 未读，1 已读。 */
    private static final int UNREAD = 0;

    /** 推送事件类型：新消息，客户端按该类型分发渲染。 */
    private static final String EVENT_MESSAGE = "MESSAGE";

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 功能：发送消息并实时推送给在线接收方。
     *
     * <p>幂等顺序不可调换：消息唯一键先拦重复提交，只有首次插入成功才插图片——
     * 图片是消息的子资源，没有独立幂等键，重发时若先插图片会随重试翻倍。
     * 重复提交直接返回原消息，不重插图片、不重推（原消息首次已推过）。
     *
     * <p>推送注册在事务提交之后执行：事务回滚时消息不存在，绝不能把不存在的消息
     * 推给接收方（推送不可撤回，比晚推严重得多）。
     *
     * @param request 发送请求，含会话 ID、文本、图片地址列表与幂等键
     * @return 落库后的消息；重复提交时返回首次落库的原消息
     * @throws BusinessException 内容与图片同时为空抛 400，会话不存在或非参与者抛 404/403，
     *                          幂等键被他人占用抛 409，登录失效抛 401
     */
    @Override
    @Transactional
    public MessageVO send(MessageSendRequest request) {
        Long userId = requireCurrentUserId();
        String messageText = stringUtils.normalizeText(request.messageText());
        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        if (messageText == null && imageUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容与图片不能同时为空");
        }
        Conversation conversation = requireParticipant(request.conversationId(), userId);
        Long receiveUserId = peerOf(conversation, userId);

        Message message = new Message();
        message.setMessageConversationId(conversation.getConversationId());
        message.setMessageSendUserId(userId);
        message.setMessageReceiveUserId(receiveUserId);
        message.setMessageText(messageText);
        message.setMessageIsRead(UNREAD);
        message.setMessageIdempotencyKey(request.messageIdempotencyKey().trim());
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突：重复提交，回查原消息；图片已在首次插入时落库，此处绝不重插
            Message existing = messageMapper.selectOne(Wrappers.<Message>lambdaQuery()
                    .eq(Message::getMessageIdempotencyKey, message.getMessageIdempotencyKey())
                    .last("LIMIT 1"));
            if (existing == null || !existing.getMessageSendUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "消息幂等键已被使用");
            }
            return toMessageVO(existing, loadImageUrls(existing.getMessageId()));
        }
        insertImages(message.getMessageId(), imageUrls);
        // 回查一次拿到数据库生成的创建时间，避免推送给前端的时间字段为空
        Message saved = messageMapper.selectById(message.getMessageId());
        MessageVO messageVO = toMessageVO(saved, imageUrls);
        pushAfterCommit(receiveUserId, messageVO);
        return messageVO;
    }

    /** 查询会话内消息列表，按消息 ID 倒序游标分页，图片一次批量装载避免逐条回查。 */
    @Override
    public MessagePageVO listByConversation(MessageQueryRequest request) {
        Long userId = requireCurrentUserId();
        requireParticipant(request.conversationId(), userId);
        int pageSize = resolvePageSize(request.pageSize());
        List<Message> messages = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getMessageConversationId, request.conversationId())
                .lt(request.cursorId() != null, Message::getMessageId, request.cursorId())
                .orderByDesc(Message::getMessageId)
                .last("LIMIT " + pageSize));
        return toMessagePage(messages, pageSize);
    }

    /** 拉取当前用户全部未读消息，接收方向全量扫描由 (接收者, 已读, 会话) 联合索引支撑。 */
    @Override
    public MessagePageVO pullUnread(UnreadQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        List<Message> messages = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getMessageReceiveUserId, userId)
                .eq(Message::getMessageIsRead, UNREAD)
                .lt(request.cursorId() != null, Message::getMessageId, request.cursorId())
                .orderByDesc(Message::getMessageId)
                .last("LIMIT " + pageSize));
        return toMessagePage(messages, pageSize);
    }

    /**
     * 功能：把某会话中当前用户的未读消息批量标记为已读。
     *
     * <p>批量条件更新本身即幂等：重复调用第二次影响 0 行。不逐条处理——
     * 已读标记是展示型状态位，没有"状态与另一张表联动"的账目问题，批量安全。
     */
    @Override
    @Transactional
    public int markRead(Long conversationId) {
        Long userId = requireCurrentUserId();
        requireParticipant(conversationId, userId);
        return messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .set(Message::getMessageIsRead, 1)
                .eq(Message::getMessageConversationId, conversationId)
                .eq(Message::getMessageReceiveUserId, userId)
                .eq(Message::getMessageIsRead, UNREAD));
    }

    /**
     * 功能：在事务提交后向接收方推送消息，无事务时立即推送。
     *
     * <p>推送失败由推送层静默降级（更记日志），不反噬发送主流程——
     * 消息已落库，接收方离线或推送失败都能通过上线拉取补偿。
     */
    private void pushAfterCommit(Long receiveUserId, MessageVO messageVO) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            realtimePushService.sendToUser(receiveUserId, EVENT_MESSAGE, messageVO);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimePushService.sendToUser(receiveUserId, EVENT_MESSAGE, messageVO);
            }
        });
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
        boolean participant = userId.equals(conversation.getConversationUserAId())
                || userId.equals(conversation.getConversationUserBId());
        if (!participant) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    /** 取会话中当前用户之外的另一个参与者，即消息接收者。 */
    private Long peerOf(Conversation conversation, Long userId) {
        return userId.equals(conversation.getConversationUserAId())
                ? conversation.getConversationUserBId()
                : conversation.getConversationUserAId();
    }

    /** 逐张插入消息图片，sort 按提交顺序从 0 递增。 */
    private void insertImages(Long messageId, List<String> imageUrls) {
        int sort = 0;
        for (String imageUrl : imageUrls) {
            MessageImage image = new MessageImage();
            image.setMessageImageMessageId(messageId);
            image.setMessageImageUrl(imageUrl);
            image.setMessageImageSort(sort++);
            messageImageMapper.insert(image);
        }
    }

    /** 批量装载当页消息的图片地址，一次 IN 查询分组映射，避免逐条回查。 */
    private Map<Long, List<String>> loadImageUrls(List<Message> messages) {
        if (messages.isEmpty()) {
            return Map.of();
        }
        List<Long> messageIds = messages.stream().map(Message::getMessageId).toList();
        List<MessageImage> images = messageImageMapper.selectList(Wrappers.<MessageImage>lambdaQuery()
                .in(MessageImage::getMessageImageMessageId, messageIds)
                .orderByAsc(MessageImage::getMessageImageSort));
        return images.stream().collect(Collectors.groupingBy(MessageImage::getMessageImageMessageId,
                Collectors.mapping(MessageImage::getMessageImageUrl, Collectors.toList())));
    }

    /** 查询单条消息的图片地址，按展示顺序升序。 */
    private List<String> loadImageUrls(Long messageId) {
        return messageImageMapper.selectList(Wrappers.<MessageImage>lambdaQuery()
                        .eq(MessageImage::getMessageImageMessageId, messageId)
                        .orderByAsc(MessageImage::getMessageImageSort))
                .stream()
                .map(MessageImage::getMessageImageUrl)
                .toList();
    }

    /** 把消息列表组装为分页返回对象，取满一页时以末行消息 ID 作为下一页游标。 */
    private MessagePageVO toMessagePage(List<Message> messages, int pageSize) {
        Map<Long, List<String>> imageMap = loadImageUrls(messages);
        List<MessageVO> list = messages.stream()
                .map(message -> toMessageVO(message, imageMap.getOrDefault(message.getMessageId(), List.of())))
                .toList();
        if (list.size() < pageSize) {
            return new MessagePageVO(list, false, null);
        }
        return new MessagePageVO(list, true, messages.get(messages.size() - 1).getMessageId());
    }

    /** 将消息实体转换为接口返回对象。 */
    private MessageVO toMessageVO(Message message, List<String> imageUrls) {
        return new MessageVO(
                message.getMessageId(),
                message.getMessageConversationId(),
                message.getMessageSendUserId(),
                message.getMessageReceiveUserId(),
                message.getMessageText(),
                imageUrls,
                message.getMessageIsRead(),
                message.getCreateTime()
        );
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
}
