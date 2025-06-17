package com.campushub.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 新增日志请求参数。 */
public record LogCreateRequest(
        Long logUserId,

        @NotBlank(message = "用户请求地址不能为空")
        @Size(max = 45, message = "用户请求地址长度不能超过 45 个字符")
        String logUserIp,

        @NotBlank(message = "操作类型不能为空")
        @Size(max = 32, message = "操作类型长度不能超过 32 个字符")
        String logType,

        Long logTableId,

        @NotNull(message = "操作状态不能为空")
        @Min(value = 0, message = "操作状态只能为 0 或 1")
        @Max(value = 1, message = "操作状态只能为 0 或 1")
        Integer logStatus,

        @Size(max = 65535, message = "日志说明长度不能超过 65535 个字符")
        String logText
) {
}
