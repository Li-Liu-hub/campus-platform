package com.campushub.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户表实体。 */
@Data
@TableName("ch_user")
public class User {

    /** 用户主键，使用雪花算法生成。 */
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;

    /** 用户登录账号。 */
    @TableField("user_name")
    private String userName;

    /** 用户密码，保存加密后的密码。 */
    @TableField("user_password")
    private String userPassword;

    /** 用户昵称。 */
    @TableField("user_nickname")
    private String userNickname;

    /** 用户权限或角色标识。 */
    @TableField("user_role")
    private String userRole;

    /** 用户账户余额。 */
    @TableField("user_amount")
    private BigDecimal userAmount;

    /** 用户个性签名。 */
    @TableField("user_personal_signature")
    private String userPersonalSignature;

    /** 删除标识：0 表示未删除，1 表示已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("user_is_delete")
    private Integer userIsDelete;

    /** 用户创建时间。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 用户最后更新时间。 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

}
