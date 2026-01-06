package com.alex.ai.security;

import com.alex.ai.config.ApiSecurityProperties;
import com.alex.ai.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * API 限流切面
 * 
 * 基于 Bucket4j 实现令牌桶限流算法
 * 
 * @author Alex
 * @since 2026-01-05
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final ApiSecurityProperties securityProperties;

    /**
     * 限流桶缓存（按 key 存储）
     * 使用 Caffeine 缓存，自动过期清理
     */
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();

    /**
     * 环绕通知：拦截带有 @RateLimit 注解的方法
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return joinPoint.proceed();
        }

        String key = buildKey(joinPoint, rateLimit);
        Bucket bucket = resolveBucket(key, rateLimit.requestsPerMinute());

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            log.warn("请求被限流: key={}", key);
            throw BusinessException.tooManyRequests("请求过于频繁，请稍后再试");
        }
    }

    /**
     * 构建限流 key
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String prefix = rateLimit.keyPrefix().isEmpty() 
            ? signature.getMethod().getName() 
            : rateLimit.keyPrefix();

        if (rateLimit.perIp()) {
            String clientIp = getClientIp();
            return prefix + ":" + clientIp;
        }
        return prefix;
    }

    /**
     * 获取或创建限流桶
     */
    private Bucket resolveBucket(String key, int requestsPerMinute) {
        return bucketCache.get(key, k -> createBucket(requestsPerMinute));
    }

    /**
     * 创建令牌桶
     * 使用 Bucket4j 新版 API（简化的 builder 模式）
     */
    private Bucket createBucket(int requestsPerMinute) {
        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1)))
            .build();
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        // 优先从代理头获取真实 IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
