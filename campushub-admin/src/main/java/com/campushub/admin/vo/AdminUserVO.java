package com.campushub.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户列表行。
 *
 * <p>用 @Data 类而非 record：字段由 SQL 直接映射填充（无装配逻辑），
 * 与 user 模块 LoginVO 的风格一致。
 */
@Data
public class AdminUserVO {

    /** 用户 ID。 */
    private Long userId;

    /** 登录账号。 */
    private String userName;

    /** 用户昵称。 */
    private String userNickname;

    /** 角色编码（USER/ADMIN），关联 ch_role.role_code。 */
    private String userRole;

    /** 账户余额。 */
    private BigDecimal userAmount;

    /** 个性签名。 */
    private String userPersonalSignature;

    /** 注册时间。 */
    private LocalDateTime createTime;
}
