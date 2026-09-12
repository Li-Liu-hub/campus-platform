package com.campushub.message.controller;

import com.campushub.common.response.Result;
import com.campushub.message.dto.MessageQueryRequest;
import com.campushub.message.dto.MessageSendRequest;
import com.campushub.message.dto.UnreadQueryRequest;
import com.campushub.message.service.MessageService;
import com.campushub.message.vo.MessagePageVO;
import com.campushub.message.vo.MessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息基础接口。
 *
 * <p>发送刻意走 HTTP 而非 WebSocket：幂等键、登录拦截、统一异常与可观测性
 * 全部复用现有 HTTP 治理，WebSocket 只承担服务端到客户端的实时推送。
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 发送消息：落库成功后向在线接收方实时推送，离线等接收方上线拉取。 */
    @PostMapping("/send")
    public Result<MessageVO> send(@Valid @RequestBody MessageSendRequest request) {
        return Result.success(messageService.send(request));
    }

    /** 游标分页查询会话内消息，按消息 ID 倒序。 */
    @GetMapping("/list")
    public Result<MessagePageVO> list(@Valid @ModelAttribute MessageQueryRequest request) {
        return Result.success(messageService.listByConversation(request));
    }

    /** 拉取当前用户的全部未读消息（上线补偿路径）。 */
    @GetMapping("/unread")
    public Result<MessagePageVO> unread(@Valid @ModelAttribute UnreadQueryRequest request) {
        return Result.success(messageService.pullUnread(request));
    }

    /** 把某会话中当前用户的未读消息标记为已读，返回本次标记条数。 */
    @PostMapping("/read/{conversationId}")
    public Result<Integer> read(@PathVariable Long conversationId) {
        return Result.success(messageService.markRead(conversationId));
    }
}
