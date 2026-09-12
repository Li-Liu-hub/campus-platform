package com.campushub.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端帖子列表行，含发帖人昵称（SQL 直查组装，不依赖帖子模块代码）。 */
@Data
public class AdminPostVO {

    /** 帖子 ID。 */
    private Long postId;

    /** 帖子标题。 */
    private String postTitle;

    /** 帖子分类。 */
    private String postType;

    /** 发帖用户 ID。 */
    private Long postUserId;

    /** 发帖人昵称，发帖人已注销时为空。 */
    private String postUserNickname;

    /** 浏览量。 */
    private Long postViewNumber;

    /** 点赞数。 */
    private Long postLikeNumber;

    /** 收藏数。 */
    private Long postCollectNumber;

    /** 发布时间。 */
    private LocalDateTime createTime;
}
