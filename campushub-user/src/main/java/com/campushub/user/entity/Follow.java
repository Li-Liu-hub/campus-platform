package com.campushub.user.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注表实体。
 *
 * <p>关注是明细关系（插入/删除语义），无软删与更新时间；
 * 重复关注由唯一键吸收为幂等成功，取关不存在也幂等。
 */
@Data
@TableName("ch_follow")
public class Follow {

    /** 关注主键，使用雪花算法生成。 */
    @TableId(value = "follow_id", type = IdType.ASSIGN_ID)
    private Long followId;

    /** 发出关注的用户 ID。 */
    @TableField("follow_sent_user_id")
    private Long followSentUserId;

    /** 被关注的用户 ID。 */
    @TableField("follow_receive_user_id")
    private Long followReceiveUserId;

    /** 创建时间由数据库维护，新增 SQL 不写入该字段。 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
