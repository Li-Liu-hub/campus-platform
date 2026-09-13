package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.user.dto.UpdatePasswordRequest;
import com.campushub.user.dto.UpdateSignatureRequest;
import com.campushub.user.entity.User;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.service.UserService;
import com.campushub.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 用户信息业务实现。 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final AuthenticationService authenticationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 查询当前登录用户的用户名称和个性签名。 */
    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserId, userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        return UserInfoVO.builder()
                .userId(user.getUserId())
                .userNickname(user.getUserNickname())
                .userPersonalSignature(user.getUserPersonalSignature())
                .build();
    }

    /**
     * 修改个性签名：用显式 SET 而非实体更新。
     *
     * <p>MyBatis-Plus 默认策略会跳过 null 字段，实体更新无法把签名“清空”为 NULL；
     * 这里明确区分“传空串 = 清空（写 NULL）”。
     */
    @Override
    public void updateSignature(UpdateSignatureRequest request) {
        Long userId = requireCurrentUserId();
        String signature = request.userPersonalSignature();
        if (signature != null) {
            signature = signature.trim();
            if (signature.isEmpty()) {
                signature = null;
            }
        }
        int updated = userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getUserPersonalSignature, signature)
                .eq(User::getUserId, userId));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    /** 修改密码：BCrypt 校验原密码，成功写新密文后踢出全部登录设备。 */
    @Override
    public void updatePassword(UpdatePasswordRequest request) {
        Long userId = requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码错误");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getUserPassword, passwordEncoder.encode(request.newPassword()))
                .eq(User::getUserId, userId));
        // 改密后旧凭证全部失效，强制所有设备重新登录
        authenticationService.kickout(userId);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }
}
