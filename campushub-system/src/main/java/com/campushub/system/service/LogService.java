package com.campushub.system.service;

import com.campushub.common.log.LogEvent;
import com.campushub.system.dto.LogCreateRequest;
import com.campushub.system.dto.LogUpdateRequest;
import com.campushub.system.vo.LogVO;

/** 日志基础 CRUD 业务接口。 */
public interface LogService {

    /** 创建日志。 */
    LogVO create(LogCreateRequest request);

    /** 根据日志 ID 查询日志。 */
    LogVO getById(Long logId);

    /** 修改日志。 */
    LogVO update(Long logId, LogUpdateRequest request);

    /** 删除日志。 */
    void delete(Long logId);

    /**
     * 功能：写入消费端异步落库的操作日志事件。
     *
     * @param event 操作日志事件，来自消息队列反序列化，create_time 使用事件携带的操作时刻
     */
    void insert(LogEvent event);
}
