package com.campushub.post.service;

import com.campushub.post.vo.RankItemVO;
import com.campushub.post.vo.RankVO;

import java.util.List;

/** 帖子热度榜查询接口，数据源为 Redis ZSet 日榜/周榜，MySQL 全程无感知。 */
public interface PostRankService {

    /**
     * 功能：查询当日热度榜前 N 名。
     *
     * @param n 名次数量，缺省 10，上限 100
     * @return 按热度分倒序的榜单条目，榜为空时返回空列表
     */
    List<RankItemVO> top(Integer n);

    /**
     * 功能：查询近 7 日周榜前 N 名，周榜由日榜 ZUNIONSTORE 合成并缓存 10 分钟。
     *
     * @param n 名次数量，缺省 10，上限 100
     * @return 按热度分倒序的榜单条目，榜为空时返回空列表
     */
    List<RankItemVO> week(Integer n);

    /**
     * 功能：查询指定帖子的当日名次与热度分。
     *
     * @param postId 帖子 ID，不允许为空
     * @return 名次与分数，未上榜时两者均为 null
     */
    RankVO rankOf(Long postId);
}
