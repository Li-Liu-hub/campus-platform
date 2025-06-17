package com.campushub.admin.controller;

import com.campushub.common.response.Result;
import com.campushub.system.dto.LogCreateRequest;
import com.campushub.system.dto.LogUpdateRequest;
import com.campushub.system.service.LogService;
import com.campushub.system.vo.LogVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员日志基础 CRUD 接口，不增加用户权限区分。 */
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /** 创建日志。 */
    @PostMapping
    public Result<LogVO> create(@Valid @RequestBody LogCreateRequest request) {
        return Result.success(logService.create(request));
    }

    /** 根据日志 ID 查询日志。 */
    @GetMapping("/{logId}")
    public Result<LogVO> getById(@PathVariable Long logId) {
        return Result.success(logService.getById(logId));
    }

    /** 修改日志。 */
    @PutMapping("/{logId}")
    public Result<LogVO> update(@PathVariable Long logId,
                                @Valid @RequestBody LogUpdateRequest request) {
        return Result.success(logService.update(logId, request));
    }

    /** 删除日志。 */
    @DeleteMapping("/{logId}")
    public Result<Void> delete(@PathVariable Long logId) {
        logService.delete(logId);
        return Result.success();
    }
}
