package com.campushub.infrastructure.security;

import java.time.Instant;

/**
 * 对外返回的登录设备会话信息，不包含完整登录 Token。
 *
 * @param sessionId 设备会话标识，用于单设备下线
 * @param deviceId 客户端设备唯一标识
 * @param deviceType 设备类型
 * @param deviceName 设备展示名称
 * @param loginTime 登录时间
 * @param isCurrent 是否为当前请求使用的登录设备
 */
public record LoginDeviceSession(
        String sessionId,
        String deviceId,
        String deviceType,
        String deviceName,
        Instant loginTime,
        boolean isCurrent
) {
}
