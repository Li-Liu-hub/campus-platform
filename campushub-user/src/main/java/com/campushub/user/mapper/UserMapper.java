package com.campushub.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户表数据访问接口。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
