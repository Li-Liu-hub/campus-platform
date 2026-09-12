package com.campushub.admin.constant;

/**
 * 角色编码常量，与 ch_role.role_code、ch_user.user_role 取值对齐。
 *
 * <p>用户与角色的关联键是编码字符串而非角色 ID（user_role 列先于角色表存在），
 * 因此编码一旦被数据引用便不可随意变更，集中在此定义避免散落裸字符串。
 */
public final class RoleCodes {

    /** 普通用户：注册后的默认角色。 */
    public static final String USER = "USER";

    /** 管理员：可访问数据查看等管理端接口。 */
    public static final String ADMIN = "ADMIN";

    /** 常量类，禁止实例化。 */
    private RoleCodes() {
    }
}
