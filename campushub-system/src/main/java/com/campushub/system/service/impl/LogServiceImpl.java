package com.campushub.system.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.log.LogEvent;
import com.campushub.system.dto.LogCreateRequest;
import com.campushub.system.dto.LogUpdateRequest;
import com.campushub.system.entity.Log;
import com.campushub.system.mapper.LogMapper;
import com.campushub.system.service.LogService;
import com.campushub.system.vo.LogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** 日志基础 CRUD 业务实现，不区分用户权限。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;

    /** 创建日志并返回数据库生成时间。管理端直写路径同样逐条生成事件号，保证与消费端共用同一唯一键语义。 */
    @Override
    public LogVO create(LogCreateRequest request) {
        Log log = new Log();
        log.setLogEventId(UUID.randomUUID().toString());
        log.setLogUserId(request.logUserId());
        log.setLogUserIp(request.logUserIp().trim());
        log.setLogType(request.logType().trim());
        log.setLogTargetId(request.logTargetId());
        log.setLogStatus(request.logStatus());
        log.setLogText(request.logText());
        log.setLogCostTime(request.logCostTime());
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
        log.setLogTargetId(request.logTargetId());
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

    /**
     * 功能：将操作日志事件幂等写入日志表，create_time 使用事件携带的操作发生时刻而非落库时刻。
     *
     * <p>幂等语义：按事件唯一号吸收重复投递——同一事件重投撞唯一键 uk_log_event_id，
     * 捕获后视为已消费按成功返回，触发容器 ACK 让 broker 删除消息，“行已存在 = 已消费”，
     * 业务表即账本。事件号在生成端逐条生成，因此同一秒内对同一目标的多次不同操作是不同事件，
     * 不会像旧 (log_type, log_target_id, create_time) 复合键那样因秒级精度被误判丢弃。
     *
     * @param event 操作日志事件，由消息队列消费端传入
     * @throws Exception 插入失败且非唯一键冲突时抛出，触发容器本地重试与死信兜底
     */
    @Override
    public void insert(LogEvent event) {
        Log entity = new Log();
        // 升级窗口内队列存量旧格式消息不带事件号，兜底生成以免撞 NOT NULL：这类消息重投去重不生效，日志重复无害
        entity.setLogEventId(event.eventId() == null ? UUID.randomUUID().toString() : event.eventId());
        entity.setLogUserId(event.userId());
        entity.setLogUserIp(event.ip());
        entity.setLogType(event.type());
        entity.setLogTargetId(event.targetId());
        entity.setLogStatus(event.success() ? 1 : 0);
        entity.setLogText(event.text());
        entity.setLogCostTime(event.costTime());
        entity.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(event.operateTime()), ZoneId.systemDefault()));
        try {
            logMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            log.info("操作日志重复投递已吸收，eventId={}，type={}，targetId={}",
                    event.eventId(), event.type(), event.targetId());
        }
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
                log.getLogTargetId(),
                log.getLogStatus(),
                log.getLogText(),
                log.getCreateTime(),
                log.getLogCostTime()
        );
    }
}
