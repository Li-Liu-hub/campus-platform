package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.infrastructure.security.LoginDevice;
import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.entity.User;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.service.AuthService;
import com.campushub.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 用户认证业务实现。 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final AuthenticationService authenticationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 功能：校验用户名唯一性并创建用户，同时使用 BCrypt 加密保存密码。
     *
     * @param request 注册账号和密码，账号与密码已由控制器完成基础格式校验
     * @throws BusinessException 用户名已存在时抛出 409 业务异常
     */
    @Override
    public void register(RegisterDTO request) {
        String userName = request.getUserName().trim();
        User existingUser = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUserName(userName);
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserNickname(userName);
        user.setUserRole("USER");
        user.setUserAmount(BigDecimal.ZERO);
        user.setUserIsDelete(0);
        userMapper.insert(user);
    }

    /**
     * 功能：根据用户名查询用户并校验密码，校验通过后根据设备信息创建登录状态。
     *
     * @param request 登录账号和密码，账号与密码已由控制器完成非空校验
     * @param device 登录设备信息，由控制器从请求头提取
     * @return 登录成功后的用户信息和登录凭证
     * @throws BusinessException 用户不存在或密码错误时抛出 401 业务异常
     */
    @Override
    public LoginVO login(LoginDTO request, LoginDevice device) {
        String userName = request.getUserName().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        if (user == null || !passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String tokenValue = authenticationService.login(user.getUserId(), device);
        return LoginVO.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userNickname(user.getUserNickname())
                .tokenValue(tokenValue)
                .build();
    }

    /** 注销当前登录状态。 */
    @Override
    public void logout() {
        authenticationService.logout();
    }
}
