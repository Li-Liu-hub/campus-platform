package com.campushub.notification.vo;

import java.time.LocalDateTime;

/** 通知接口返回对象。 */
public record NotificationVO(
        Long notificationId,
        String notificationType,
        String notificationText,
        Integer notificationIsRead,
        LocalDateTime createTime
) {
}
