package com.campushub.infrastructure.security;

import java.util.List;

/**
 * 功能：定义系统登录会话的创建、销毁、查询和校验能力，隔离业务模块与具体鉴权框架。
 */
public interface AuthenticationService {

    /**
     * 功能：根据用户主键和设备信息创建登录会话并返回登录凭证。
     *
     * @param userId 用户主键，不允许为空
     * @param device 登录设备信息，设备 ID 不允许为空
     * @return 登录凭证，供客户端在后续请求中携带
     * @throws IllegalArgumentException userId 或 device 为空时抛出
     */
    String login(Long userId, LoginDevice device);

    /** 注销当前请求对应的登录会话。 */
    void logout();

    /**
     * 功能：踢出指定用户的全部登录设备，并保留“被踢下线”的状态。
     *
     * @param userId 用户主键，不允许为空
     * @throws IllegalArgumentException userId 为空时抛出
     */
    void kickout(Long userId);

    /** 校验当前请求是否存在有效的登录会话。 */
    void checkLogin();

    /**
     * 获取当前请求对应的用户主键。
     *
     * @return 当前用户主键；当前请求未登录时返回 null
     */
    Long getCurrentUserId();

    /**
     * 功能：查询指定用户的登录设备列表，并返回不包含完整 Token 的会话信息。
     *
     * @param userId 用户主键，不允许为空
     * @return 登录设备列表，顺序与登录时间顺序一致
     * @throws IllegalArgumentException userId 为空时抛出
     */
    List<LoginDeviceSession> getLoginDevices(Long userId);

    /**
     * 根据设备会话标识踢出单个登录设备。
     *
     * @param userId 用户主键，不允许为空
     * @param sessionId 对外返回的设备会话标识，不允许为空
     * @throws IllegalArgumentException 参数为空或设备会话不存在时抛出
     */
    void kickoutBySessionId(Long userId, String sessionId);
}
