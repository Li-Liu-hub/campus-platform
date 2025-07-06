package com.campushub.post.vo;

import java.time.LocalDateTime;

/**
 * 榜单条目返回对象：榜单分数与帖子概要信息合一。
 *
 * @param postId 帖子 ID
 * @param postTitle 帖子标题
 * @param postType 帖子分类
 * @param postViewNumber 帖子浏览量（缓存快照，允许分钟级滞后）
 * @param createTime 帖子创建时间
 * @param score 热度分（浏览 1 / 点赞 3 / 收藏 5 加权）
 * @param rank 名次，从 1 开始
 */
public record RankItemVO(
        Long postId,
        String postTitle,
        String postType,
        Long postViewNumber,
        LocalDateTime createTime,
        double score,
        long rank
) {
}
