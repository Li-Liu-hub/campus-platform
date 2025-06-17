package com.campushub.system.vo;

import java.time.LocalDateTime;

/** 日志接口返回对象。 */
public record LogVO(
        Long logId,
        Long logUserId,
        String logUserIp,
        String logType,
        Long logTableId,
        Integer logStatus,
        String logText,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
