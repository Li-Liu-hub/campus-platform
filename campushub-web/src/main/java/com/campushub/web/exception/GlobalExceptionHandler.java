package com.campushub.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/** 统一处理 Web 请求异常，并输出标准响应结构。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 功能：处理业务规则校验失败产生的异常，并保留其业务码和提示。
     *
     * @param exception 业务异常，包含公共错误码和可展示提示
     * @return HTTP 状态码与业务码一致的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return failure(errorCode.getCode(), exception.getMessage());
    }

    /**
     * 功能：处理请求体字段校验失败，并返回首条可读的校验提示。
     *
     * @param exception 请求体参数校验异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        String message = firstMessage(exception.getBindingResult().getAllErrors());
        return failure(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 功能：处理表单或查询对象绑定校验失败，并返回首条可读的校验提示。
     *
     * @param exception 参数绑定异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        String message = firstMessage(exception.getBindingResult().getAllErrors());
        return failure(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 功能：处理控制器方法参数约束校验失败，并返回首条可读的校验提示。
     *
     * @param exception 控制器方法参数校验异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleMethodValidation(
            HandlerMethodValidationException exception) {
        String message = firstMessage(exception.getAllErrors());
        return failure(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 功能：处理 Jakarta Validation 直接抛出的约束校验异常。
     *
     * @param exception 参数约束校验异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return failure(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 功能：处理请求体缺失或 JSON 格式无法解析的异常。
     *
     * @param exception 请求体解析异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException exception) {
        return failure(HttpStatus.BAD_REQUEST.value(), "请求体格式错误");
    }

    /**
     * 功能：处理查询参数或路径参数类型转换失败的异常。
     *
     * @param exception 方法参数类型转换异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return failure(HttpStatus.BAD_REQUEST.value(), "请求参数格式错误：" + exception.getName());
    }

    /**
     * 功能：处理缺少必要查询参数、请求头等请求绑定异常。
     *
     * @param exception Servlet 请求绑定异常
     * @return 状态码为 400 的统一失败响应
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Result<Void>> handleRequestBinding(
            ServletRequestBindingException exception) {
        return failure(HttpStatus.BAD_REQUEST.value(), ErrorCode.BAD_REQUEST.getMessage());
    }

    /**
     * 功能：处理缺少、失效或已过期的 Sa-Token 登录凭证。
     *
     * @param exception Sa-Token 未登录异常
     * @return 状态码为 401 的统一失败响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException exception) {
        return failure(HttpStatus.UNAUTHORIZED.value(), "登录状态已失效，请重新登录");
    }

    /**
     * 功能：处理 Sa-Token 角色或权限校验失败的异常。
     *
     * @param exception Sa-Token 权限异常
     * @return 状态码为 403 的统一失败响应
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<Result<Void>> handleForbidden(SaTokenException exception) {
        return failure(HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN.getMessage());
    }

    /**
     * 功能：兼容 Spring 显式状态异常，保留异常指定的 HTTP 状态和安全提示。
     *
     * @param exception Spring HTTP 状态异常
     * @return 使用异常状态码的统一失败响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = StringUtils.hasText(exception.getReason())
                ? exception.getReason()
                : defaultMessage(status);
        return failure(status, message);
    }

    /**
     * 功能：兜底处理未被前述规则匹配的异常。Spring Web 标准异常保留其状态码，
     * 其他未知异常记录完整日志并向调用方隐藏内部实现细节。
     *
     * @param exception 请求处理过程中抛出的异常
     * @param request 当前 HTTP 请求，用于记录请求方法和路径
     * @return Spring 标准错误对应的失败响应，或状态码为 500 的统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception,
                                                         HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            return failure(status, defaultMessage(status));
        }

        log.error("请求处理发生未预期异常，method={}，path={}",
                request.getMethod(), request.getRequestURI(), exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    /** 从校验错误中提取首条可读提示。 */
    private String firstMessage(Collection<? extends MessageSourceResolvable> errors) {
        return errors.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
    }

    /** 根据 HTTP 状态码生成可安全展示的默认提示。 */
    private String defaultMessage(int status) {
        return switch (status) {
            case 400 -> ErrorCode.BAD_REQUEST.getMessage();
            case 401 -> ErrorCode.UNAUTHORIZED.getMessage();
            case 403 -> ErrorCode.FORBIDDEN.getMessage();
            case 404 -> ErrorCode.NOT_FOUND.getMessage();
            case 405 -> "请求方法不支持";
            case 406 -> "无法生成客户端可接受的响应格式";
            case 409 -> ErrorCode.CONFLICT.getMessage();
            case 415 -> "请求媒体类型不支持";
            case 429 -> "请求过于频繁，请稍后重试";
            default -> status >= 500 ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : "请求处理失败";
        };
    }

    /** 创建 HTTP 状态码与业务码一致的失败响应。 */
    private ResponseEntity<Result<Void>> failure(int status, String message) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(status);
        return ResponseEntity.status(statusCode).body(Result.failure(status, message));
    }
}
