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

    /** 将指定未删除帖子的浏览量原子加一，并发下不会丢失计数。 */
    void increaseViewNumber(@Param("postId") Long postId);
}
