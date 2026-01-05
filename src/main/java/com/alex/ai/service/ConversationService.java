package com.alex.ai.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话服务 - 支持上下文记忆和 RAG 知识增强
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ChatModel chatModel;
    
    @Autowired(required = false)
    private StreamingChatModel streamingChatModel;
    
    @Autowired(required = false)
    private KnowledgeService knowledgeService;
    
    // 会话存储 (sessionId -> 消息历史)
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();
    
    // 会话 RAG 开关 (sessionId -> enableRag)
    private final Map<String, Boolean> sessionRagEnabled = new ConcurrentHashMap<>();
    
    // 最大历史消息数
    private static final int MAX_HISTORY = 20;

    /**
     * 创建新的会话
     * 
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    private List<ChatMessage> createSession(String sessionId) {
        log.info("创建新会话: {}", sessionId);
        List<ChatMessage> messages = new ArrayList<>();
        // 添加系统提示词
        messages.add(SystemMessage.from("你是一个友好、专业的 AI 助手，能够帮助用户解决各种问题。当提供了参考资料时，请优先基于参考资料回答。"));
        // 注意：不在这里调用 sessions.put()，由 computeIfAbsent 自动处理
        return messages;
    }

    /**
     * 获取或创建会话
     * 
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    private List<ChatMessage> getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, this::createSession);
    }

    /**
     * 设置会话的 RAG 开关
     * 
     * @param sessionId 会话 ID
     * @param enabled 是否启用 RAG
     */
    public void setRagEnabled(String sessionId, boolean enabled) {
        sessionRagEnabled.put(sessionId, enabled);
        log.info("会话 {} RAG 状态: {}", sessionId, enabled ? "启用" : "禁用");
    }

    /**
     * 检查会话是否启用 RAG
     */
    public boolean isRagEnabled(String sessionId) {
        return sessionRagEnabled.getOrDefault(sessionId, true); // 默认启用
    }

    /**
     * 对用户输入进行 RAG 增强处理
     * 
     * @param sessionId 会话 ID
     * @param input 用户原始输入
     * @return RAG 增强后的输入（如果有相关知识）或原始输入
     */
    private String processWithRag(String sessionId, String input) {
        if (knowledgeService == null || !isRagEnabled(sessionId)) {
            return input;
        }
        
        try {
            // 直接尝试 RAG 增强，让向量库决定是否有相关内容
            // 不再检查 totalEntries()，因为元数据存储在内存中会在重启后丢失
            // 而向量数据持久化在 Chroma 中，可以正常检索
            String augmented = knowledgeService.buildAugmentedPrompt(input);
            if (!augmented.equals(input)) {
                log.info("已应用 RAG 增强，会话: {}", sessionId);
            }
            return augmented;
        } catch (Exception e) {
            log.warn("RAG 处理异常，使用原始输入: {}", e.getMessage());
            return input;
        }
    }

    /**
     * 继续对话
     * 
     * @param sessionId 会话 ID
     * @param input 用户输入
     * @return AI 响应
     */
    public String continueConversation(String sessionId, String input) {
        log.info("会话 {} 接收消息: {}", sessionId, input);
        try {
            // 获取会话历史
            List<ChatMessage> messages = getOrCreateSession(sessionId);
            
            // RAG 增强处理
            String processedInput = processWithRag(sessionId, input);
            
            // 添加用户消息
            messages.add(UserMessage.from(processedInput));
            
            // 限制历史消息数量（保留系统消息 + 最近的对话）
            if (messages.size() > MAX_HISTORY) {
                List<ChatMessage> trimmed = new ArrayList<>();
                trimmed.add(messages.get(0)); // 保留系统消息
                trimmed.addAll(messages.subList(messages.size() - MAX_HISTORY + 1, messages.size()));
                messages.clear();
                messages.addAll(trimmed);
            }
            
            // 调用 AI 模型 (LangChain4j 1.x: chat() 返回 ChatResponse)
            ChatResponse response = chatModel.chat(messages);
            String aiResponse = response.aiMessage().text();
            
            // 添加 AI 响应到历史
            messages.add(response.aiMessage());
            
            log.info("会话 {} 响应成功", sessionId);
            return aiResponse;
            
        } catch (Exception e) {
            log.error("会话 {} 处理失败: {}", sessionId, e.getMessage(), e);
            return "抱歉，处理您的请求时遇到了问题：" + e.getMessage();
        }
    }

    /**
     * 流式对话 - 支持 SSE 实时返回
     * 
     * @param sessionId 会话 ID
     * @param input 用户输入
     * @param emitter SSE 发送器
     */
    public void streamConversation(String sessionId, String input, SseEmitter emitter) {
        log.info("会话 {} 接收流式消息: {}", sessionId, input);
        
        if (streamingChatModel == null) {
            // 如果不支持流式，使用传统方式
            try {
                String response = continueConversation(sessionId, input);
                emitter.send(SseEmitter.event().data(response));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送响应失败", e);
                emitter.completeWithError(e);
            }
            return;
        }
        
        try {
            // 获取会话历史
            List<ChatMessage> messages = getOrCreateSession(sessionId);
            
            // RAG 增强处理
            String processedInput = processWithRag(sessionId, input);
            
            // 添加用户消息
            messages.add(UserMessage.from(processedInput));
            
            // 限制历史消息数量
            if (messages.size() > MAX_HISTORY) {
                List<ChatMessage> trimmed = new ArrayList<>();
                trimmed.add(messages.get(0)); // 保留系统消息
                trimmed.addAll(messages.subList(messages.size() - MAX_HISTORY + 1, messages.size()));
                messages.clear();
                messages.addAll(trimmed);
            }
            
            // 用于收集完整响应的缓冲区
            StringBuilder fullResponse = new StringBuilder();
            
            // 调用流式 API (LangChain4j 1.x: 使用 StreamingChatResponseHandler)
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    try {
                        fullResponse.append(partialResponse);
                        // 发送每个 token，并立即刷新
                        emitter.send(SseEmitter.event().data(partialResponse));
                        log.debug("发送 token: {}", partialResponse);
                    } catch (IOException e) {
                        log.error("发送 token 失败", e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    try {
                        // 添加 AI 响应到历史
                        messages.add(response.aiMessage());
                        // 发送完成信号
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                        log.info("会话 {} 流式响应完成，总长度: {}", sessionId, fullResponse.length());
                    } catch (IOException e) {
                        log.error("完成信号发送失败", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("会话 {} 流式处理失败", sessionId, error);
                    emitter.completeWithError(error);
                }
            });
            
        } catch (Exception e) {
            log.error("会话 {} 处理失败: {}", sessionId, e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 清除会话
     * 
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        log.info("清除会话: {}", sessionId);
        sessions.remove(sessionId);
    }

    /**
     * 获取会话数量
     * 
     * @return 当前活跃会话数
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取所有会话 ID
     * 
     * @return 会话 ID 列表
     */
    public java.util.Set<String> getSessionIds() {
        return sessions.keySet();
    }

    /**
     * 检查会话是否存在
     * 
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
