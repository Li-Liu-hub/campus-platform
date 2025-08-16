package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 帖子表数据访问接口。 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 功能：按游标分页查询未删除帖子，结果按 (create_time, post_id) 排序。
     *
     * @param postType 帖子分类，可选
     * @param postTitle 标题前缀关键字，可选
     * @param orderByAsc true 按创建时间正序，false 按创建时间倒序
     * @param cursorTime 上一页末行创建时间，首页传 null
     * @param cursorId 上一页末行帖子 ID，与 cursorTime 成对使用，首页传 null
     * @param pageSize 本页行数上限
     * @return 最多 pageSize 条帖子
     */
    List<Post> selectByCondition(@Param("postType") String postType,
                                 @Param("postTitle") String postTitle,
                                 @Param("orderByAsc") boolean orderByAsc,
                                 @Param("cursorTime") LocalDateTime cursorTime,
                                 @Param("cursorId") Long cursorId,
                                 @Param("pageSize") int pageSize);

    /**
     * 功能：将指定帖子的浏览量原子增加指定增量，供定时刷库任务从 Redis 折叠落库。
     *
     * @param postId 帖子主键
     * @param delta 浏览增量，大于 0
     * @return 受影响行数，0 表示帖子已不存在
     */
    int increaseViewNumberBy(@Param("postId") Long postId, @Param("delta") long delta);

    /**
     * 功能：按增量调整帖子点赞数聚合字段，供互动事件消费者异步维护。
     *
     * <p>聚减时以 GREATEST 兜底，计数不会出现负值。
     *
     * @param postId 帖子主键
     * @param delta 计数增量，点赞 +1，取消点赞 -1
     * @return 受影响行数，0 表示帖子已不存在
     */
    int adjustLikeNumber(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 功能：按增量调整帖子收藏数聚合字段，语义与点赞数调整一致。
     *
     * @param postId 帖子主键
     * @param delta 计数增量，收藏 +1，取消收藏 -1
     * @return 受影响行数，0 表示帖子已不存在
     */
    int adjustCollectNumber(@Param("postId") Long postId, @Param("delta") int delta);
}
