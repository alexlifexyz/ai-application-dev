package com.alex.ai.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 聊天服务 - 基础对话功能
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;

    /**
     * 简单对话 - 单轮问答
     * 
     * @param userInput 用户输入
     * @return AI 响应
     */
    public String chat(String userInput) {
        log.info("接收到用户消息: {}", userInput);
        try {
            // LangChain4j 1.x: 使用 chat() 方法代替 generate()
            var response = chatModel.chat(userInput);
            log.info("AI 响应: {}", response);
            return response;
        } catch (Exception e) {
            log.warn("聊天服务调用失败: {}", e.getMessage());
            return "⚠️ 服务当前处于演示模式\n\n" +
                   "请配置有效的 OpenAI API Key：\n" +
                   "1. export OPENAI_API_KEY=your-api-key\n" +
                   "2. 重启应用：mvn spring-boot:run\n\n" +
                   "您的消息：" + userInput;
        }
    }

    /**
     * 带系统提示词的对话
     * 
     * @param systemPrompt 系统提示词（定义 AI 角色和行为）
     * @param userInput 用户输入
     * @return AI 响应
     */
    public String chatWithContext(String systemPrompt, String userInput) {
        log.info("系统提示词: {}, 用户消息: {}", systemPrompt, userInput);
        try {
            // LangChain4j 1.x: 使用 chat() 方法，传入消息列表
            var response = chatModel.chat(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userInput)
            );
            String result = response.aiMessage().text();
            log.info("AI 响应: {}", result);
            return result;
        } catch (Exception e) {
            log.warn("聊天服务调用失败: {}", e.getMessage());
            return "⚠️ 服务当前处于演示模式\n\n" +
                   "请配置有效的 OpenAI API Key 后重启应用。\n" +
                   "系统提示词：" + systemPrompt + "\n" +
                   "您的消息：" + userInput;
        }
    }
}
