package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.user.dto.LoginDTO;
import com.campushub.user.dto.RegisterDTO;
import com.campushub.user.entity.User;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.service.AuthService;
import com.campushub.user.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/** 用户认证业务实现。 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 当前登录用户在 HTTP 会话中的属性名。 */
    private static final String LOGIN_USER_ID = "campushub.login.user-id";

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 功能：校验用户名唯一性并创建用户，同时使用 BCrypt 加密保存密码。
     *
     * @param request 注册账号和密码，账号与密码已由控制器完成基础格式校验
     * @throws ResponseStatusException 用户名已存在时抛出 409 异常
     */
    @Override
    public void register(RegisterDTO request) {
        String userName = request.getUserName().trim();
        User existingUser = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        if (existingUser != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
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
     * 功能：根据用户名查询用户并校验密码，校验通过后将用户 ID 保存到 HTTP 会话。
     *
     * @param request 登录账号和密码，账号与密码已由控制器完成非空校验
     * @param httpRequest 当前 HTTP 请求，用于创建登录会话
     * @return 登录成功后的用户信息
     * @throws ResponseStatusException 用户不存在或密码错误时抛出 401 异常
     */
    @Override
    public LoginVO login(LoginDTO request, HttpServletRequest httpRequest) {
        String userName = request.getUserName().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        if (user == null || !passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(LOGIN_USER_ID, user.getUserId());
        return LoginVO.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userNickname(user.getUserNickname())
                .build();
    }

    /** 注销当前 HTTP 登录会话。 */
    @Override
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
