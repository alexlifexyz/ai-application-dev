package com.alex.ai.security;

import java.lang.annotation.*;

/**
 * API 限流注解
 * 
 * 用于标记需要限流的接口方法
 * 
 * @author Alex
 * @since 2026-01-05
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每分钟最大请求数
     */
    int requestsPerMinute() default 60;

    /**
     * 限流 key 前缀（默认使用方法名）
     */
    String keyPrefix() default "";

    /**
     * 是否按 IP 限流
     */
    boolean perIp() default true;
}
