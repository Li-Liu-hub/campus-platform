package com.campushub.common.exception;

/** 公共错误码，业务码与 HTTP 状态码保持一致。 */
public enum ErrorCode {

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录状态已失效"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "请求资源不存在"),
    CONFLICT(409, "数据状态冲突"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 获取错误业务码。 */
    public int getCode() {
        return code;
    }

    /** 获取默认错误提示。 */
    public String getMessage() {
        return message;
    }
}
