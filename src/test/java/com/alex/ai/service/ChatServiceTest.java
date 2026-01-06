package com.alex.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChatService 单元测试
 * 
 * @author Alex
 * @since 2026-01-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 单元测试")
class ChatServiceTest {

    @Mock
    private ChatModel chatModel;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatModel);
    }

    @Test
    @DisplayName("简单对话 - 正常响应")
    void chat_shouldReturnResponse_whenModelSucceeds() {
        // Given
        String userInput = "你好";
        String expectedResponse = "你好！有什么可以帮助你的？";
        when(chatModel.chat(anyString())).thenReturn(expectedResponse);

        // When
        String result = chatService.chat(userInput);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(chatModel).chat(userInput);
    }

    @Test
    @DisplayName("简单对话 - 模型异常时返回演示响应")
    void chat_shouldReturnDemoResponse_whenModelFails() {
        // Given
        String userInput = "你好";
        when(chatModel.chat(anyString())).thenThrow(new RuntimeException("API 调用失败"));

        // When
        String result = chatService.chat(userInput);

        // Then
        assertThat(result).contains("演示模式");
        assertThat(result).contains(userInput);
    }

    @Test
    @DisplayName("带系统提示词对话 - 正常响应")
    void chatWithContext_shouldReturnResponse_whenModelSucceeds() {
        // Given
        String systemPrompt = "你是一位 Java 专家";
        String userInput = "什么是多态？";
        String expectedResponse = "多态是面向对象编程的重要特性...";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        AiMessage aiMessage = AiMessage.from(expectedResponse);
        when(mockResponse.aiMessage()).thenReturn(aiMessage);
        when(chatModel.chat(any(SystemMessage.class), any(UserMessage.class))).thenReturn(mockResponse);

        // When
        String result = chatService.chatWithContext(systemPrompt, userInput);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(chatModel).chat(any(SystemMessage.class), any(UserMessage.class));
    }

    @Test
    @DisplayName("带系统提示词对话 - 模型异常时返回演示响应")
    void chatWithContext_shouldReturnDemoResponse_whenModelFails() {
        // Given
        String systemPrompt = "你是一位 Java 专家";
        String userInput = "什么是多态？";
        when(chatModel.chat(any(SystemMessage.class), any(UserMessage.class)))
            .thenThrow(new RuntimeException("API 调用失败"));

        // When
        String result = chatService.chatWithContext(systemPrompt, userInput);

        // Then
        assertThat(result).contains("演示模式");
        assertThat(result).contains(systemPrompt);
        assertThat(result).contains(userInput);
    }

    @Test
    @DisplayName("空输入处理")
    void chat_shouldHandleEmptyInput() {
        // Given
        String userInput = "";
        when(chatModel.chat(anyString())).thenReturn("请输入您的问题");

        // When
        String result = chatService.chat(userInput);

        // Then
        assertThat(result).isNotNull();
        verify(chatModel).chat(userInput);
    }
}
