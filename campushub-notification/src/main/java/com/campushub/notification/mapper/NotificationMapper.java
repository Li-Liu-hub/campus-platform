package com.campushub.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/** 通知表数据访问接口。 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
