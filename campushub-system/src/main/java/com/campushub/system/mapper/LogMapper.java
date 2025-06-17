package com.campushub.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.system.entity.Log;
import org.apache.ibatis.annotations.Mapper;

/** 日志表数据访问接口。 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {
}
