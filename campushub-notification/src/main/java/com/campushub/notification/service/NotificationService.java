package com.campushub.notification.service;

import com.campushub.notification.dto.NotificationQueryRequest;
import com.campushub.notification.vo.NotificationVO;
import com.campushub.notification.vo.PageVO;

/**
 * 通知业务接口。
 *
 * <p>写入侧（create）供关注、帖子互动等触发方调用；读取侧（query/unreadCount/read/delete）
 * 面向通知接收者本人，所有读操作只作用于当前登录用户自己的通知。
 */
public interface NotificationService {

    /**
     * 功能：创建一条通知（供触发方调用）。
     *
     * @param recipientUserId 接收者用户 ID
     * @param notificationType 通知类型，取值见 NotificationTypes
     * @param notificationText 通知内容
     */
    void create(Long recipientUserId, String notificationType, String notificationText);

    /** 分页查询当前用户的通知（含已读），按通知 ID 倒序。 */
    PageVO<NotificationVO> query(NotificationQueryRequest request);

    /** 查询当前用户的未读通知数。 */
    long unreadCount();

    /** 把当前用户的一条通知标记为已读（幂等：已读状态重复标记无副作用）。 */
    void markRead(Long notificationId);

    /** 把当前用户的全部未读通知标记为已读，返回本次标记条数。 */
    int markAllRead();

    /** 删除当前用户的一条通知（物理删除，仅限本人）。 */
    void delete(Long notificationId);
}
