package com.alex.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * ConversationService 单元测试
 * 
 * @author Alex
 * @since 2026-01-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService 单元测试")
class ConversationServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private KnowledgeService knowledgeService;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(chatModel);
        // 注入 knowledgeService（通过反射或 setter）
        try {
            var field = ConversationService.class.getDeclaredField("knowledgeService");
            field.setAccessible(true);
            field.set(conversationService, knowledgeService);
        } catch (Exception e) {
            // 忽略，某些测试不需要 knowledgeService
        }
    }

    @Test
    @DisplayName("多轮对话 - 新建会话")
    void continueConversation_shouldCreateNewSession() {
        // Given
        String sessionId = "test-session-001";
        String userInput = "你好";
        String expectedResponse = "你好！有什么可以帮助你的？";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        AiMessage aiMessage = AiMessage.from(expectedResponse);
        when(mockResponse.aiMessage()).thenReturn(aiMessage);
        when(chatModel.chat(anyList())).thenReturn(mockResponse);

        // When
        String result = conversationService.continueConversation(sessionId, userInput);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(chatModel).chat(anyList());
    }

    @Test
    @DisplayName("多轮对话 - 保持会话上下文")
    void continueConversation_shouldMaintainContext() {
        // Given
        String sessionId = "test-session-002";
        
        ChatResponse mockResponse1 = mock(ChatResponse.class);
        when(mockResponse1.aiMessage()).thenReturn(AiMessage.from("我是 AI 助手"));
        
        ChatResponse mockResponse2 = mock(ChatResponse.class);
        when(mockResponse2.aiMessage()).thenReturn(AiMessage.from("我刚才说我是 AI 助手"));
        
        when(chatModel.chat(anyList()))
            .thenReturn(mockResponse1)
            .thenReturn(mockResponse2);

        // When - 第一轮对话
        String result1 = conversationService.continueConversation(sessionId, "你是谁？");
        
        // When - 第二轮对话
        String result2 = conversationService.continueConversation(sessionId, "你刚才说什么？");

        // Then
        assertThat(result1).isEqualTo("我是 AI 助手");
        assertThat(result2).contains("AI 助手");
        verify(chatModel, times(2)).chat(anyList());
    }

    @Test
    @DisplayName("多轮对话 - 不同会话隔离")
    void continueConversation_shouldIsolateSessions() {
        // Given
        String sessionId1 = "session-A";
        String sessionId2 = "session-B";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.aiMessage()).thenReturn(AiMessage.from("OK"));
        when(chatModel.chat(anyList())).thenReturn(mockResponse);

        // When
        conversationService.continueConversation(sessionId1, "消息1");
        conversationService.continueConversation(sessionId2, "消息2");

        // Then - 验证两次调用都创建了独立的消息历史
        verify(chatModel, times(2)).chat(anyList());
    }

    @Test
    @DisplayName("RAG 开关 - 启用/禁用")
    void setRagEnabled_shouldToggleRag() {
        // Given
        String sessionId = "rag-test-session";
        
        // When & Then - 默认启用
        assertThat(conversationService.isRagEnabled(sessionId)).isTrue();
        
        // When - 禁用
        conversationService.setRagEnabled(sessionId, false);
        
        // Then
        assertThat(conversationService.isRagEnabled(sessionId)).isFalse();
        
        // When - 重新启用
        conversationService.setRagEnabled(sessionId, true);
        
        // Then
        assertThat(conversationService.isRagEnabled(sessionId)).isTrue();
    }

    @Test
    @DisplayName("清除会话 - 正常流程")
    void clearSession_shouldRemoveSessionData() {
        // Given
        String sessionId = "clear-test-session";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.aiMessage()).thenReturn(AiMessage.from("OK"));
        when(chatModel.chat(anyList())).thenReturn(mockResponse);
        
        // 先创建会话
        conversationService.continueConversation(sessionId, "测试消息");
        
        // When
        conversationService.clearSession(sessionId);
        
        // Then - 再次对话会创建新会话
        conversationService.continueConversation(sessionId, "新消息");
        verify(chatModel, times(2)).chat(anyList());
    }

    @Test
    @DisplayName("多轮对话 - 异常处理")
    void continueConversation_shouldHandleException() {
        // Given
        String sessionId = "error-session";
        when(chatModel.chat(anyList())).thenThrow(new RuntimeException("API 错误"));

        // When
        String result = conversationService.continueConversation(sessionId, "测试");

        // Then
        assertThat(result).contains("抱歉");
        assertThat(result).contains("问题");
    }
}
