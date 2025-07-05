package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.PostCollect;
import org.apache.ibatis.annotations.Mapper;

/** 帖子收藏表数据访问接口，基础增删由 MyBatis-Plus 内置方法提供。 */
@Mapper
public interface PostCollectMapper extends BaseMapper<PostCollect> {
}
