package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.user.entity.User;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.service.UserService;
import com.campushub.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 用户信息业务实现。 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final AuthenticationService authenticationService;

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
}
