package com.campushub.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/** 消息表数据访问接口。 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
