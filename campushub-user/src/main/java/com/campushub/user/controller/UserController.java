package com.campushub.user.controller;

import com.campushub.common.response.Result;
import com.campushub.user.dto.UpdatePasswordRequest;
import com.campushub.user.dto.UpdateSignatureRequest;
import com.campushub.user.service.UserService;
import com.campushub.user.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户信息相关接口。 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户的名称和个性签名。 */
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        return Result.success(userService.getCurrentUserInfo());
    }

    /** 修改个性签名（传空串清空）。 */
    @PutMapping("/signature")
    public Result<Void> updateSignature(@Valid @RequestBody UpdateSignatureRequest request) {
        userService.updateSignature(request);
        return Result.success();
    }

    /** 修改密码：校验原密码，成功后踢出全部登录设备需重新登录。 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request);
        return Result.success();
    }
}
