package com.alex.ai.controller;

import com.alex.ai.model.ChatRequest;
import com.alex.ai.model.ChatResponse;
import com.alex.ai.security.RateLimit;
import com.alex.ai.service.ChatService;
import com.alex.ai.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天接口控制器
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Tag(name = "AI 聊天服务", description = "提供 AI 对话相关的 REST API")
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    /**
     * 简单对话接口
     * 
     * @param request 聊天请求
     * @return 聊天响应
     */
    @Operation(summary = "简单对话", description = "单轮问答，不保留上下文")
    @PostMapping("/simple")
    @RateLimit(requestsPerMinute = 30, keyPrefix = "chat-simple")
    public ResponseEntity<ChatResponse> simpleChat(@Valid @RequestBody ChatRequest request) {
        log.info("收到简单对话请求: {}", request.getMessage());
        String response = chatService.chat(request.getMessage());
        return ResponseEntity.ok(ChatResponse.success(response));
    }

    /**
     * 带系统提示词的对话接口
     * 
     * @param request 聊天请求（包含系统提示词）
     * @return 聊天响应
     */
    @Operation(summary = "上下文对话", description = "支持自定义系统提示词的对话")
    @PostMapping("/with-context")
    @RateLimit(requestsPerMinute = 30, keyPrefix = "chat-context")
    public ResponseEntity<ChatResponse> chatWithContext(@Valid @RequestBody ChatRequest request) {
        log.info("收到上下文对话请求");
        String systemPrompt = request.getSystemPrompt() != null 
            ? request.getSystemPrompt() 
            : "你是一个友好、专业的 AI 助手。";
        
        String response = chatService.chatWithContext(systemPrompt, request.getMessage());
        return ResponseEntity.ok(ChatResponse.success(response));
    }

    /**
     * 多轮对话接口（支持上下文）
     * 
     * @param request 聊天请求（包含会话 ID）
     * @return 聊天响应
     */
    @Operation(summary = "多轮对话", description = "支持上下文记忆的多轮对话，通过 sessionId 区分会话")
    @PostMapping("/conversation")
    @RateLimit(requestsPerMinute = 30, keyPrefix = "chat-conversation")
    public ResponseEntity<ChatResponse> conversation(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null 
            ? request.getSessionId() 
            : "default-session";
        
        log.info("会话 {} 收到消息", sessionId);
        String response = conversationService.continueConversation(sessionId, request.getMessage());
        return ResponseEntity.ok(ChatResponse.success(response, sessionId));
    }

    /**
     * 流式多轮对话接口（SSE）
     * 
     * @param request 聊天请求（包含会话 ID）
     * @return SSE 流
     */
    @Operation(summary = "流式多轮对话", description = "支持上下文记忆的流式对话，实时返回 AI 响应")
    @PostMapping(value = "/conversation/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(requestsPerMinute = 20, keyPrefix = "chat-stream")
    public SseEmitter streamConversation(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null 
            ? request.getSessionId() 
            : "default-session";
        
        log.info("会话 {} 收到流式消息", sessionId);
        
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 异步处理
        new Thread(() -> {
            conversationService.streamConversation(sessionId, request.getMessage(), emitter);
        }).start();
        
        return emitter;
    }

    /**
     * 清除会话
     * 
     * @param sessionId 会话 ID
     * @return 操作结果
     */
    @Operation(summary = "清除会话", description = "删除指定会话的上下文记忆")
    @DeleteMapping("/conversation/{sessionId}")
    public ResponseEntity<ChatResponse> clearConversation(
        @Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        log.info("清除会话: {}", sessionId);
        conversationService.clearSession(sessionId);
        return ResponseEntity.ok(ChatResponse.success("会话已清除", sessionId));
    }

    /**
     * 健康检查接口
     */
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Chat Service is running!");
    }

    /**
     * 获取系统信息
     */
    @Operation(summary = "获取系统信息", description = "返回服务版本、状态等信息")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "AI Chat Service");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("timestamp", LocalDateTime.now());
        info.put("features", Arrays.asList("简单对话", "上下文对话", "多轮对话", "会话管理"));
        return ResponseEntity.ok(info);
    }

    /**
     * 获取所有活跃会话列表
     */
    @Operation(summary = "获取会话列表", description = "返回当前所有活跃会话的 ID 列表")
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", conversationService.getSessionCount());
        result.put("sessions", conversationService.getSessionIds());
        result.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定会话的信息
     */
    @Operation(summary = "获取会话信息", description = "查询指定会话是否存在")
    @GetMapping("/conversation/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionInfo(
        @Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("exists", conversationService.sessionExists(sessionId));
        info.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(info);
    }

    /**
     * 获取配置信息
     */
    @Operation(summary = "获取配置信息", description = "返回当前服务的配置和支持的操作")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("mode", "demo"); // 演示模式
        config.put("maxMessages", 10);
        config.put("endpoint", "/api/chat");
        config.put("supportedOperations", Arrays.asList(
            "GET /health - 健康检查",
            "GET /info - 系统信息",
            "GET /sessions - 会话列表",
            "GET /config - 配置信息",
            "POST /simple - 简单对话",
            "POST /with-context - 上下文对话",
            "POST /conversation - 多轮对话",
            "DELETE /conversation/{id} - 清除会话"
        ));
        return ResponseEntity.ok(config);
    }
}
