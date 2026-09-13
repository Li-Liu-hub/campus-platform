package com.campushub.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注/粉丝列表行。
 *
 * <p>用 @Data 类而非 record：字段由 SQL 直接映射填充；
 * followId 为分页游标内部用途（与关注时间组成二元组），前端可忽略。
 */
@Data
public class FollowUserVO {

    /** 对方用户 ID（关注列表里是被关注者，粉丝列表里是关注者）。 */
    private Long userId;

    /** 对方昵称。 */
    private String userNickname;

    /** 对方个性签名。 */
    private String userPersonalSignature;

    /** 关注关系建立时间，游标分页依据。 */
    private LocalDateTime followTime;

    /** 关注关系 ID：与 followTime 组成游标二元组，前端可忽略。 */
    private Long followId;
}
