package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.PostImage;
import org.apache.ibatis.annotations.Mapper;

/** 帖子图片表数据访问接口。 */
@Mapper
public interface PostImageMapper extends BaseMapper<PostImage> {
}
