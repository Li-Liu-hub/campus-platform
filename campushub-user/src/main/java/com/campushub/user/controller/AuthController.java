package com.campushub.user.controller;

import com.campushub.common.response.Result;
import com.campushub.infrastructure.security.LoginDevice;
import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.service.AuthService;
import com.campushub.user.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户认证相关接口，包括登录、注册和退出。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 功能：校验用户登录信息并根据客户端设备信息创建登录状态。
     *
     * @param request 登录账号和密码，账号与密码均不允许为空
     * @param deviceId 客户端生成并持久保存的设备唯一标识，通过 X-Device-Id 请求头传入
     * @param deviceType 设备类型，通过 X-Device-Type 请求头传入，默认使用 WEB
     * @param deviceName 设备展示名称，使用 User-Agent 请求头传入
     * @return 包含登录用户信息的统一成功响应
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO request,
                                 @RequestHeader("X-Device-Id") String deviceId,
                                 @RequestHeader(value = "X-Device-Type", defaultValue = "WEB") String deviceType,
                                 @RequestHeader(value = "User-Agent", defaultValue = "UNKNOWN") String deviceName) {
        LoginDevice device = new LoginDevice(deviceType, deviceId, deviceName);
        return Result.success(authService.login(request, device));
    }

    /**
     * 功能：校验注册信息并创建用户账号。
     *
     * @param request 注册账号和密码，账号长度不超过 64 个字符，密码长度为 6 到 64 个字符
     * @return 不携带业务数据的统一成功响应
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO request) {
        authService.register(request);
        return Result.success();
    }

    /**
     * 功能：注销当前用户的 Sa-Token 登录状态，使当前令牌失效。
     *
     * @return 不携带业务数据的统一成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}
