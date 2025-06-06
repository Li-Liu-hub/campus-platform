package com.campushub.common.response;

/**
 * 功能：统一封装接口响应，使成功和失败响应使用一致的数据结构。
 *
 * @param <T> 响应数据类型；无响应数据时使用 {@link Void}
 */
public final class Result<T> {

    /** 成功响应使用的业务码。 */
    public static final int SUCCESS_CODE = 200;

    /** 成功响应使用的默认提示。 */
    public static final String SUCCESS_MESSAGE = "操作成功";

    private final int code;

    private final String message;

    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 创建不携带数据的成功响应。 */
    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 功能：创建携带业务数据的成功响应。
     *
     * @param data 返回给调用方的业务数据，允许为空
     * @param <T> 业务数据类型
     * @return 业务码为 200 的统一成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 功能：创建不携带业务数据的失败响应。
     *
     * @param code 错误业务码，当前与对应 HTTP 状态码保持一致
     * @param message 可安全展示给调用方的错误提示
     * @param <T> 响应数据类型
     * @return 指定错误码和提示的统一失败响应
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 获取业务码。 */
    public int getCode() {
        return code;
    }

    /** 获取响应提示。 */
    public String getMessage() {
        return message;
    }

    /** 获取响应数据。 */
    public T getData() {
        return data;
    }
}
