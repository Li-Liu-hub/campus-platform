package com.campushub.infrastructure.mq.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** MQ 发件箱表数据访问接口。 */
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
}
