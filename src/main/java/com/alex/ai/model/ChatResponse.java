package com.alex.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天响应 DTO
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Schema(description = "聊天响应对象")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 响应状态码
     */
    @Schema(description = "响应状态码", example = "200")
    private int code;

    /**
     * AI 响应消息
     */
    @Schema(description = "AI 响应的消息内容", example = "你好！我是 AI 助手...")
    private String message;

    /**
     * 会话 ID
     */
    @Schema(description = "会话 ID（多轮对话时返回）", example = "user-123")
    private String sessionId;

    /**
     * 响应时间戳
     */
    @Schema(description = "响应时间戳", example = "2025-12-31T15:00:00")
    private LocalDateTime timestamp;

    /**
     * 构建成功响应
     */
    public static ChatResponse success(String message) {
        return new ChatResponse(200, message, null, LocalDateTime.now());
    }

    /**
     * 构建成功响应（带会话 ID）
     */
    public static ChatResponse success(String message, String sessionId) {
        return new ChatResponse(200, message, sessionId, LocalDateTime.now());
    }

    /**
     * 构建失败响应
     */
    public static ChatResponse error(String message) {
        return new ChatResponse(500, message, null, LocalDateTime.now());
    }
}
