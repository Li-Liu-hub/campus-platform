package com.campushub.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.post.entity.PostView;
import org.apache.ibatis.annotations.Mapper;

/** 帖子浏览明细 Mapper：仅追加明细行，聚合统计走 ch_post.post_view_number 折叠。 */
@Mapper
public interface PostViewMapper extends BaseMapper<PostView> {
}
