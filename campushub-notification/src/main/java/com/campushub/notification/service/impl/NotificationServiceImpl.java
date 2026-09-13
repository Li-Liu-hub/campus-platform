package com.campushub.notification.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.notification.dto.NotificationQueryRequest;
import com.campushub.notification.entity.Notification;
import com.campushub.notification.mapper.NotificationMapper;
import com.campushub.notification.service.NotificationService;
import com.campushub.notification.vo.NotificationVO;
import com.campushub.notification.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知业务实现。
 *
 * <p>写入是单向流水（不做幂等去重，重复触发天然多条）；读取侧全部按
 * “当前登录用户的自己的数据”收口：列表、未读数、已读、删除都带 user_id 条件，
 * 单条操作先查归属再落库，杜绝越权。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    private final AuthenticationService authenticationService;

    /** 未读标识：0 未读，1 已读。 */
    private static final int UNREAD = 0;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 创建一条通知：参数不完整时静默忽略，通知是辅助信息、不能反噬触发方主流程。 */
    @Override
    public void create(Long recipientUserId, String notificationType, String notificationText) {
        if (recipientUserId == null || notificationType == null || notificationText == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setNotificationUserId(recipientUserId);
        notification.setNotificationType(notificationType);
        notification.setNotificationText(notificationText);
        notification.setNotificationIsRead(UNREAD);
        notificationMapper.insert(notification);
    }

    /** 分页查询当前用户的通知（含已读），按通知 ID 倒序游标翻页。 */
    @Override
    public PageVO<NotificationVO> query(NotificationQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        List<Notification> records = notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getNotificationUserId, userId)
                .lt(request.cursorId() != null, Notification::getNotificationId, request.cursorId())
                .orderByDesc(Notification::getNotificationId)
                .last("LIMIT " + pageSize));
        List<NotificationVO> list = records.stream().map(this::toNotificationVO).toList();
        if (list.size() < pageSize) {
            return new PageVO<>(list, false, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        return new PageVO<>(list, true, records.get(records.size() - 1).getNotificationId());
    }

    /** 当前用户未读通知数，走 (用户, 已读, ID) 联合索引。 */
    @Override
    public long unreadCount() {
        Long userId = requireCurrentUserId();
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getNotificationUserId, userId)
                .eq(Notification::getNotificationIsRead, UNREAD));
        return count == null ? 0L : count;
    }

    /** 单条标记已读：已读状态重复标记直接返回（幂等），未读时条件更新。 */
    @Override
    public void markRead(Long notificationId) {
        Long userId = requireCurrentUserId();
        Notification notification = requireOwned(notificationId, userId);
        if (notification.getNotificationIsRead() != null && notification.getNotificationIsRead() != UNREAD) {
            return;
        }
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .set(Notification::getNotificationIsRead, 1)
                .eq(Notification::getNotificationId, notificationId)
                .eq(Notification::getNotificationUserId, userId));
    }

    /** 全部标记已读：批量条件更新，返回本次实际标记条数。 */
    @Override
    public int markAllRead() {
        Long userId = requireCurrentUserId();
        return notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .set(Notification::getNotificationIsRead, 1)
                .eq(Notification::getNotificationUserId, userId)
                .eq(Notification::getNotificationIsRead, UNREAD));
    }

    /** 删除本人的一条通知（物理删除）。 */
    @Override
    public void delete(Long notificationId) {
        Long userId = requireCurrentUserId();
        requireOwned(notificationId, userId);
        notificationMapper.deleteById(notificationId);
    }

    /** 查询通知并校验归属，不存在抛 404，非本人抛 403。 */
    private Notification requireOwned(Long notificationId, Long userId) {
        if (notificationId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知 ID 不能为空");
        }
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (!userId.equals(notification.getNotificationUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人的通知");
        }
        return notification;
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

    /** 将通知实体转换为接口返回对象。 */
    private NotificationVO toNotificationVO(Notification notification) {
        return new NotificationVO(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getNotificationText(),
                notification.getNotificationIsRead(),
                notification.getCreateTime()
        );
    }
}
