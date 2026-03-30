package com.aitrip.backend.ratelimit;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.common.exception.BusinessException;
import com.aitrip.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 限流 AOP 切面
 * 拦截 @RateLimit 注解的方法，执行令牌桶限流
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final TokenBucketRateLimiter rateLimiter;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        // 解析 SpEL key
        String resolvedKey = resolveKey(rateLimit.key(), pjp);
        String bucketKey = RedisKeyConstants.SECKILL_TOKEN_BUCKET + resolvedKey;

        // 判断是否 VIP（从请求头或参数中获取）
        boolean isVip = resolveIsVip(pjp);

        boolean allowed = rateLimiter.tryAcquire(
                bucketKey,
                rateLimit.capacity(),
                rateLimit.rate(),
                rateLimit.cost(),
                isVip
        );

        if (!allowed) {
            log.warn("[RateLimit] 限流触发 key={} isVip={}", bucketKey, isVip);
            throw new BusinessException(ErrorCode.RATE_LIMIT, rateLimit.message());
        }

        return pjp.proceed();
    }

    /**
     * 解析 SpEL 表达式为字符串 Key
     */
    private String resolveKey(String spelExpr, ProceedingJoinPoint pjp) {
        if (!spelExpr.startsWith("#")) {
            return spelExpr; // 非 SpEL，直接返回
        }
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Parameter[] params = method.getParameters();
            Object[] args = pjp.getArgs();

            EvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < params.length; i++) {
                ctx.setVariable(params[i].getName(), args[i]);
            }
            Expression expr = spelParser.parseExpression(spelExpr);
            Object value = expr.getValue(ctx);
            return value != null ? value.toString() : "default";
        } catch (Exception e) {
            log.warn("[RateLimit] SpEL 解析失败 expr={}, 使用 default key", spelExpr);
            return "default";
        }
    }

    /**
     * 从方法参数中尝试获取 userId，再判断 VIP 状态
     * 简化实现：参数名含 isVip/vip 时直接读取
     */
    private boolean resolveIsVip(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Parameter[] params = signature.getMethod().getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < params.length; i++) {
            String name = params[i].getName().toLowerCase();
            if ((name.contains("vip") || name.contains("isvip")) && args[i] instanceof Boolean) {
                return (Boolean) args[i];
            }
        }
        return false;
    }
}
