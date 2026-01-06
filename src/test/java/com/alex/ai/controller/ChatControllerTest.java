package com.alex.ai.controller;

import com.alex.ai.service.ChatService;
import com.alex.ai.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController 集成测试
 * 
 * @author Alex
 * @since 2026-01-05
 */
@WebMvcTest(ChatController.class)
@DisplayName("ChatController 集成测试")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ConversationService conversationService;

    @Test
    @DisplayName("POST /api/chat/simple - 简单对话成功")
    void simpleChat_shouldReturnSuccess() throws Exception {
        // Given
        String userMessage = "你好";
        String aiResponse = "你好！有什么可以帮助你的？";
        when(chatService.chat(anyString())).thenReturn(aiResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", userMessage))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value(aiResponse));
        
        verify(chatService).chat(userMessage);
    }

    @Test
    @DisplayName("POST /api/chat/simple - 消息为空返回错误")
    void simpleChat_shouldReturnError_whenMessageEmpty() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/chat/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", ""))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/chat/with-context - 带系统提示词对话")
    void chatWithContext_shouldUseSystemPrompt() throws Exception {
        // Given
        String systemPrompt = "你是 Java 专家";
        String userMessage = "什么是多态？";
        String aiResponse = "多态是面向对象的特性...";
        when(chatService.chatWithContext(anyString(), anyString())).thenReturn(aiResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/with-context")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "message", userMessage,
                    "systemPrompt", systemPrompt
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value(aiResponse));
        
        verify(chatService).chatWithContext(systemPrompt, userMessage);
    }

    @Test
    @DisplayName("POST /api/chat/conversation - 多轮对话")
    void conversation_shouldReturnWithSessionId() throws Exception {
        // Given
        String sessionId = "test-session-123";
        String userMessage = "你好";
        String aiResponse = "你好！";
        when(conversationService.continueConversation(anyString(), anyString())).thenReturn(aiResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/conversation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "message", userMessage,
                    "sessionId", sessionId
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value(aiResponse))
            .andExpect(jsonPath("$.sessionId").value(sessionId));
        
        verify(conversationService).continueConversation(sessionId, userMessage);
    }

    @Test
    @DisplayName("POST /api/chat/conversation - 无 sessionId 使用默认值")
    void conversation_shouldUseDefaultSessionId() throws Exception {
        // Given
        String userMessage = "你好";
        String aiResponse = "你好！";
        when(conversationService.continueConversation(anyString(), anyString())).thenReturn(aiResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/conversation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", userMessage))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("default-session"));
        
        verify(conversationService).continueConversation("default-session", userMessage);
    }

    @Test
    @DisplayName("DELETE /api/chat/conversation/{sessionId} - 清除会话")
    void clearConversation_shouldReturnSuccess() throws Exception {
        // Given
        String sessionId = "session-to-clear";
        doNothing().when(conversationService).clearSession(anyString());

        // When & Then
        mockMvc.perform(delete("/api/chat/conversation/{sessionId}", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("会话已清除"));
        
        verify(conversationService).clearSession(sessionId);
    }

    @Test
    @DisplayName("GET /api/chat/health - 健康检查")
    void health_shouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("AI Chat Service is running!"));
    }
}
