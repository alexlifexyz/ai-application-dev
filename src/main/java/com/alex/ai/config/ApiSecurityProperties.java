package com.alex.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * API 安全配置属性
 * 
 * @author Alex
 * @since 2026-01-05
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api.security")
public class ApiSecurityProperties {

    /**
     * 是否启用 API Key 认证
     */
    private boolean enabled = false;

    /**
     * 有效的 API Key 列表
     */
    private List<String> apiKeys = new ArrayList<>();

    /**
     * API Key 请求头名称
     */
    private String headerName = "X-API-Key";

    /**
     * 不需要认证的路径（白名单）
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
        "/api/chat/health",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/**"
    ));

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class RateLimitConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 每分钟最大请求数（全局）
         */
        private int requestsPerMinute = 60;

        /**
         * 每个 IP 每分钟最大请求数
         */
        private int requestsPerMinutePerIp = 30;

        /**
         * 聊天接口每分钟最大请求数（资源消耗大，单独限制）
         */
        private int chatRequestsPerMinute = 20;
    }
}
