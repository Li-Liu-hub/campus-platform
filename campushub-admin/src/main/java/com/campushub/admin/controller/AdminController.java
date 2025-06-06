package com.campushub.admin.controller;

import com.campushub.admin.dto.KickoutRequest;
import com.campushub.common.response.Result;
import com.campushub.infrastructure.security.AuthenticationService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员相关接口。 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuthenticationService authenticationService;

    public AdminController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 功能：踢出指定用户的全部登录设备，使该用户的登录 Token 失效。
     *
     * @param request 踢人请求，包含需要踢出下线的用户 ID
     * @return 不携带业务数据的统一成功响应
     */
    @PostMapping("/users/kickout")
    public Result<Void> kickout(@RequestBody KickoutRequest request) {
        authenticationService.kickout(request.userId());
        return Result.success();
    }
}
