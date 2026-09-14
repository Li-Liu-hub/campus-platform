package com.campushub.infrastructure.log;

import com.campushub.common.log.LogEvent;
import com.campushub.common.log.OperationLog;
import com.campushub.common.mq.MqConstants;
import com.campushub.infrastructure.mq.MessagePublisher;
import com.campushub.infrastructure.security.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 功能：操作日志切面，拦截标注 {@link OperationLog} 的业务方法，统计业务耗时并
 * 组装日志事件发布到消息队列，由消费端异步落库。
 *
 * <p>运行在 Tomcat 请求线程上：业务正常返回记成功事件，业务抛出异常记失败事件后
 * 原样向上抛出；事件发布、SpEL 求值、用户会话读取中的任何故障只记本地错误日志，
 * 不影响业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final MessagePublisher messagePublisher;

    private final AuthenticationService authenticationService;

    /** SpEL 表达式缓存，同一注解表达式只解析一次，避免每次请求重复解析。 */
    private final ConcurrentMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /** 失败原因截断长度，防止超长异常文本拖慢发布与落库。 */
    private static final int MAX_TEXT_LENGTH = 500;

    /** 用户 IP 缺省占位，非 Web 上下文或无法获取时使用。 */
    private static final String UNKNOWN_IP = "unknown";

    /**
     * 功能：环绕拦截操作日志方法，计时业务执行并发布成功或失败事件。
     *
     * @param pjp 连接点，proceed() 执行业务逻辑
     * @param operationLog 方法上标注的操作日志注解
     * @return 业务方法原始返回值
     * @throws Throwable 业务方法抛出的异常原样向上传播
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            publish(operationLog, pjp.getArgs(), true, null, start);
            return result;
        } catch (Throwable throwable) {
            publish(operationLog, pjp.getArgs(), false, throwable.getMessage(), start);
            throw throwable;
        }
    }

    /**
     * 功能：组装操作日志事件并发布到日志队列，内部任何异常静默降级为本地错误日志。
     *
     * @param operationLog 操作日志注解
     * @param args 业务方法实际参数，供 SpEL 求值目标主键
     * @param success 业务是否执行成功
     * @param text 失败原因，成功时为 null
     * @param start 业务开始时刻（nanoTime），用于计算耗时
     */
    private void publish(OperationLog operationLog, Object[] args, boolean success, String text, long start) {
        try {
            long costTime = (System.nanoTime() - start) / 1_000_000;
            Long targetId = resolveTargetId(operationLog.targetId(), args);
            LogEvent event = new LogEvent(
                    // 事件唯一号逐条生成：消费端按它吸收重复投递，同秒同目标的不同事件不会被误判
                    UUID.randomUUID().toString(),
                    operationLog.type(),
                    targetId,
                    authenticationService.getCurrentUserId(),
                    resolveClientIp(),
                    success,
                    truncate(text),
                    costTime,
                    System.currentTimeMillis());
            // 业务标识随 CorrelationData 发送，confirm 回执 nack 时凭它定位丢失的业务事件
            messagePublisher.publish(MqConstants.LOG_EXCHANGE, MqConstants.LOG_ROUTING_KEY, event,
                    "oplog:" + operationLog.type() + ":" + targetId);
        } catch (Exception exception) {
            // 日志链路故障不反噬业务：会话读取、SpEL 求值、消息发布任一失败仅记录本地日志
            log.error("操作日志发布失败，type={}", operationLog.type(), exception);
        }
    }

    /**
     * 功能：按 SpEL 表达式从方法参数中解析操作目标主键。
     *
     * @param expressionText 注解声明的表达式，以 args 变量引用参数数组，允许为空
     * @param args 业务方法实际参数
     * @return 解析出的目标主键，表达式为空、求值为 null 或解析失败时返回 null
     */
    private Long resolveTargetId(String expressionText, Object[] args) {
        if (expressionText == null || expressionText.isBlank()) {
            return null;
        }
        try {
            Expression expression = expressionCache.computeIfAbsent(
                    expressionText, expr -> new SpelExpressionParser().parseExpression(expr));
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", args);
            Object value = expression.getValue(context);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(value.toString());
        } catch (Exception exception) {
            log.warn("操作日志 targetId 解析失败，expression={}", expressionText, exception);
            return null;
        }
    }

    /** 读取当前请求的客户端 IP，优先取 X-Forwarded-For 首段，非 Web 上下文返回占位值。 */
    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // 多级代理场景下第一段为真实客户端 IP
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return UNKNOWN_IP;
    }

    /** 截断失败原因到安全长度，null 原样返回。 */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}
