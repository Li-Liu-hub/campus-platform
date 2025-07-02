package com.campushub.common.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能：标记需要记录操作日志的业务方法，切面拦截后统计耗时并组装事件异步发布，
 * 日志链路任何故障不影响业务主流程。
 *
 * <p>方法正常返回记为成功日志，抛出异常记为失败日志（异常继续向上传播，
 * 由全局异常处理器统一处理）。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作类型，取值见 OperationTypes 常量。 */
    String type();

    /**
     * 操作目标数据主键的 SpEL 表达式，上下文以 args 变量引用方法实际参数数组，
     * 例如 "#args[0]" 表示第一个参数即目标主键；留空表示本次操作无具体目标。
     */
    String targetId() default "";
}
