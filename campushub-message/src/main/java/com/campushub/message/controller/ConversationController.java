package com.campushub.message.controller;

import com.campushub.common.response.Result;
import com.campushub.message.dto.ConversationCreateRequest;
import com.campushub.message.dto.ConversationQueryRequest;
import com.campushub.message.service.ConversationService;
import com.campushub.message.vo.ConversationVO;
import com.campushub.message.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 会话基础接口。 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** 获取或创建与目标用户的会话，重复提交返回原会话。 */
    @PostMapping("/create")
    public Result<ConversationVO> create(@Valid @RequestBody ConversationCreateRequest request) {
        return Result.success(conversationService.createOrGet(request));
    }

    /** 游标分页查询当前用户参与的会话列表，携带最后消息与未读数。 */
    @GetMapping("/query")
    public Result<PageVO<ConversationVO>> query(@Valid @ModelAttribute ConversationQueryRequest request) {
        return Result.success(conversationService.query(request));
    }

    /** 查询会话详情，仅参与者可见。 */
    @GetMapping("/get/{conversationId}")
    public Result<ConversationVO> getById(@PathVariable Long conversationId) {
        return Result.success(conversationService.getById(conversationId));
    }
}
