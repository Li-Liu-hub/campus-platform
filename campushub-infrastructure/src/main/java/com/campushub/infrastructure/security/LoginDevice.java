package com.campushub.infrastructure.security;

/**
 * 登录设备信息。
 *
 * @param deviceType 设备类型，例如 WEB、APP 或小程序
 * @param deviceId 客户端生成并持久保存的设备唯一标识
 * @param deviceName 设备展示名称，通常使用 User-Agent
 */
public record LoginDevice(String deviceType, String deviceId, String deviceName) {

    /** 规范化设备信息并校验设备 ID。 */
    public LoginDevice {
        deviceType = defaultValue(deviceType, "UNKNOWN");
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("设备 ID 不能为空");
        }
        deviceId = deviceId.trim();
        deviceName = defaultValue(deviceName, "UNKNOWN");
    }

    /** 值为空白时返回默认值，否则返回去除首尾空白后的值。 */
    private static String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
