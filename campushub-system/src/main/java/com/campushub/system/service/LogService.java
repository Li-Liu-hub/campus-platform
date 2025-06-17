package com.campushub.system.service;

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
}
