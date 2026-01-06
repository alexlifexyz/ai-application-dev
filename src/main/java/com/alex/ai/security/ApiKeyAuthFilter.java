package com.alex.ai.security;

import com.alex.ai.config.ApiSecurityProperties;
import com.alex.ai.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API Key 认证过滤器
 * 
 * 仅当 api.security.enabled=true 时启用
 * 
 * @author Alex
 * @since 2026-01-05
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "api.security.enabled", havingValue = "true")
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiSecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();

        // 检查是否在白名单中
        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取 API Key
        String apiKey = request.getHeader(securityProperties.getHeaderName());

        // 验证 API Key
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("请求缺少 API Key: path={}, ip={}", requestPath, getClientIp(request));
            sendUnauthorizedResponse(response, "缺少 API Key，请在请求头中提供 " + securityProperties.getHeaderName());
            return;
        }

        if (!securityProperties.getApiKeys().contains(apiKey)) {
            log.warn("无效的 API Key: path={}, ip={}", requestPath, getClientIp(request));
            sendUnauthorizedResponse(response, "无效的 API Key");
            return;
        }

        // 认证通过
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isExcludedPath(String path) {
        return securityProperties.getExcludePaths().stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.unauthorized(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
