package com.campushub.post.vo;

/**
 * 单帖排名返回对象。
 *
 * @param postId 帖子 ID
 * @param rank 当日名次，从 1 开始；未上榜时为 null
 * @param score 当日热度分；未上榜时为 null
 */
public record RankVO(
        Long postId,
        Long rank,
        Double score
) {
}
