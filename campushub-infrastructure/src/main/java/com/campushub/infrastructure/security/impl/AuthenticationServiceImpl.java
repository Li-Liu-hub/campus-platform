package com.campushub.infrastructure.security.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.session.SaTerminalInfo;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.infrastructure.security.LoginDevice;
import com.campushub.infrastructure.security.LoginDeviceSession;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 基于 Sa-Token 实现登录会话管理，包括设备信息记录、设备列表查询和踢人下线。 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    /** Sa-Token 终端扩展数据中保存设备名称的键。 */
    private static final String DEVICE_NAME_EXTRA_KEY = "deviceName";

    /** Sa-Token 终端扩展数据中保存对外会话标识的键。 */
    private static final String SESSION_ID_EXTRA_KEY = "sessionId";

    /** 创建 Sa-Token 登录会话并返回当前会话凭证。 */
    @Override
    public String login(Long userId, LoginDevice device) {
        requireUserId(userId);
        if (device == null) {
            throw new IllegalArgumentException("登录设备信息不能为空");
        }

        String sessionId = UUID.randomUUID().toString();
        SaLoginParameter parameter = SaLoginParameter.create()
                .setDeviceType(device.deviceType())
                .setDeviceId(device.deviceId())
                .setTerminalExtra(DEVICE_NAME_EXTRA_KEY, device.deviceName())
                .setTerminalExtra(SESSION_ID_EXTRA_KEY, sessionId);
        return StpUtil.createLoginSession(userId, parameter);
    }

    /** 注销当前 Sa-Token 登录会话。 */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /** 踢出指定用户的全部登录设备。 */
    @Override
    public void kickout(Long userId) {
        requireUserId(userId);
        StpUtil.kickout(userId);
    }

    /** 校验当前请求携带的 Sa-Token 登录凭证。 */
    @Override
    public void checkLogin() {
        StpUtil.checkLogin();
    }

    /** 获取当前登录用户主键，未登录时返回 null。 */
    @Override
    public Long getCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return null;
        }
        if (loginId instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("当前登录用户 ID 格式错误", exception);
        }
    }

    /** 查询指定用户的登录设备，并转换为不暴露完整 Token 的会话对象。 */
    @Override
    public List<LoginDeviceSession> getLoginDevices(Long userId) {
        requireUserId(userId);
        String currentToken = StpUtil.getTokenValue();
        return StpUtil.getTerminalListByLoginId(userId).stream()
                .map(terminal -> toLoginDeviceSession(terminal, currentToken))
                .toList();
    }

    /** 根据对外会话标识踢出指定用户的单个登录设备。 */
    @Override
    public void kickoutBySessionId(Long userId, String sessionId) {
        requireUserId(userId);
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("会话标识不能为空");
        }

        for (SaTerminalInfo terminal : StpUtil.getTerminalListByLoginId(userId)) {
            if (sessionId.equals(resolveSessionId(terminal))) {
                StpUtil.kickoutByTokenValue(terminal.getTokenValue());
                return;
            }
        }
        throw new IllegalArgumentException("登录设备不存在");
    }

    /** 将 Sa-Token 终端信息转换为不暴露完整 Token 的设备会话对象。 */
    private LoginDeviceSession toLoginDeviceSession(SaTerminalInfo terminal, String currentToken) {
        Object deviceName = terminal.getExtra(DEVICE_NAME_EXTRA_KEY);
        return new LoginDeviceSession(
                resolveSessionId(terminal),
                terminal.getDeviceId(),
                terminal.getDeviceType(),
                deviceName == null ? "UNKNOWN" : deviceName.toString(),
                Instant.ofEpochMilli(terminal.getCreateTime()),
                Objects.equals(currentToken, terminal.getTokenValue())
        );
    }

    /** 获取终端扩展中的会话标识；兼容未记录扩展信息的历史会话。 */
    private String resolveSessionId(SaTerminalInfo terminal) {
        Object sessionId = terminal.getExtra(SESSION_ID_EXTRA_KEY);
        if (sessionId != null && !sessionId.toString().isBlank()) {
            return sessionId.toString();
        }
        return digestToken(terminal.getTokenValue());
    }

    /** 使用 Token 摘要兼容历史会话，避免对外暴露完整 Token。 */
    private String digestToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法生成登录设备会话标识", exception);
        }
    }

    /** 校验用户主键不为空，为空时抛出 IllegalArgumentException。 */
    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
    }

}
