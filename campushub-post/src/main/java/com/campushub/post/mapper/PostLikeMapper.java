package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/** 帖子点赞表数据访问接口，基础增删由 MyBatis-Plus 内置方法提供。 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
