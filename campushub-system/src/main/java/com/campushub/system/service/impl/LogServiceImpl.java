package com.campushub.system.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.system.dto.LogCreateRequest;
import com.campushub.system.dto.LogUpdateRequest;
import com.campushub.system.entity.Log;
import com.campushub.system.mapper.LogMapper;
import com.campushub.system.service.LogService;
import com.campushub.system.vo.LogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 日志基础 CRUD 业务实现，不区分用户权限。 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;

    /** 创建日志并返回数据库生成时间。 */
    @Override
    public LogVO create(LogCreateRequest request) {
        Log log = new Log();
        log.setLogUserId(request.logUserId());
        log.setLogUserIp(request.logUserIp().trim());
        log.setLogType(request.logType().trim());
        log.setLogTableId(request.logTableId());
        log.setLogStatus(request.logStatus());
        log.setLogText(request.logText());
        logMapper.insert(log);
        return getById(log.getLogId());
    }

    /** 根据日志 ID 查询日志，不存在时返回 404 业务异常。 */
    @Override
    public LogVO getById(Long logId) {
        Log log = findLog(logId);
        return toLogVO(log);
    }

    /** 修改日志业务字段，不写入数据库维护的时间字段。 */
    @Override
    public LogVO update(Long logId, LogUpdateRequest request) {
        Log log = findLog(logId);
        log.setLogUserId(request.logUserId());
        log.setLogUserIp(request.logUserIp().trim());
        log.setLogType(request.logType().trim());
        log.setLogTableId(request.logTableId());
        log.setLogStatus(request.logStatus());
        log.setLogText(request.logText());
        logMapper.updateById(log);
        return getById(logId);
    }

    /** 删除日志记录。 */
    @Override
    public void delete(Long logId) {
        Log log = findLog(logId);
        logMapper.deleteById(log.getLogId());
    }

    /** 根据日志 ID 查询日志，日志不存在时抛出 404 业务异常。 */
    private Log findLog(Long logId) {
        if (logId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日志 ID 不能为空");
        }
        Log log = logMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return log;
    }

    /** 将日志实体转换为接口返回对象。 */
    private LogVO toLogVO(Log log) {
        return new LogVO(
                log.getLogId(),
                log.getLogUserId(),
                log.getLogUserIp(),
                log.getLogType(),
                log.getLogTableId(),
                log.getLogStatus(),
                log.getLogText(),
                log.getCreateTime(),
                log.getUpdateTime()
        );
    }
}
