package com.campushub.notification.controller;

import com.campushub.common.response.Result;
import com.campushub.notification.dto.NotificationQueryRequest;
import com.campushub.notification.service.NotificationService;
import com.campushub.notification.vo.NotificationVO;
import com.campushub.notification.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口：面向通知接收者本人（列表、未读数、已读、删除）。
 *
 * <p>通知的“写入”没有对外的创建接口：通知由业务事件触发（关注、点赞、收藏），
 * 触发方在服务层调用 NotificationService.create，避免接口成为垃圾通知的入口。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 分页查询我的通知（含已读），按通知 ID 倒序。 */
    @GetMapping("/query")
    public Result<PageVO<NotificationVO>> query(@Valid @ModelAttribute NotificationQueryRequest request) {
        return Result.success(notificationService.query(request));
    }

    /** 查询我的未读通知数。 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    /** 把一条通知标记为已读（仅限本人，重复标记幂等）。 */
    @PostMapping("/read/{notificationId}")
    public Result<Void> read(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
        return Result.success();
    }

    /** 把我的全部未读通知标记为已读，返回本次标记条数。 */
    @PostMapping("/read-all")
    public Result<Integer> readAll() {
        return Result.success(notificationService.markAllRead());
    }

    /** 删除我的一条通知。 */
    @DeleteMapping("/delete/{notificationId}")
    public Result<Void> delete(@PathVariable Long notificationId) {
        notificationService.delete(notificationId);
        return Result.success();
    }
}
