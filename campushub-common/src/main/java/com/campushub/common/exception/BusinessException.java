package com.campushub.common.exception;

import java.util.Objects;

/** 由业务规则校验失败触发、可安全返回给调用方的异常。 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 功能：使用公共错误码及其默认提示创建业务异常。
     *
     * @param errorCode 公共错误码，不允许为空
     * @throws NullPointerException 错误码为空时抛出
     */
    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "错误码不能为空").getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 功能：使用公共错误码和业务场景提示创建业务异常。
     *
     * @param errorCode 公共错误码，不允许为空
     * @param message 可安全返回给调用方的业务错误提示
     * @throws NullPointerException 错误码为空时抛出
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(resolveMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    /** 获取业务异常对应的公共错误码。 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        ErrorCode requiredErrorCode = Objects.requireNonNull(errorCode, "错误码不能为空");
        return message == null || message.isBlank() ? requiredErrorCode.getMessage() : message;
    }
}
