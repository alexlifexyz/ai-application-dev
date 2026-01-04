package com.alex.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求 DTO
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Schema(description = "聊天请求对象")
@Data
public class ChatRequest {

    /**
     * 用户消息（必填）
     */
    @Schema(description = "用户消息内容", example = "你好，介绍一下自己", required = true)
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 会话 ID（多轮对话时使用）
     */
    @Schema(description = "会话 ID（多轮对话时使用）", example = "user-123")
    private String sessionId;

    /**
     * 系统提示词（可选）
     */
    @Schema(description = "系统提示词（定义 AI 角色）", example = "你是一位 Java 编程导师")
    private String systemPrompt;
}
