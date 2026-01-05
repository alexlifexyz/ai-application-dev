package com.alex.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置类
 * 
 * @author Alex
 * @since 2025-12-31
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key:demo}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-3.5-turbo}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;

    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private Integer maxTokens;

    // ========== Embedding 模型配置 ==========
    
    @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-v3}")
    private String embeddingModelName;
    
    @Value("${langchain4j.open-ai.embedding-model.timeout:30s}")
    private Duration embeddingTimeout;

    // ========== 向量存储配置 ==========
    
    @Value("${rag.vector-store.type:memory}")
    private String vectorStoreType;
    
    @Value("${rag.vector-store.chroma.base-url:http://localhost:8000}")
    private String chromaBaseUrl;
    
    @Value("${rag.vector-store.chroma.collection-name:ai-knowledge}")
    private String chromaCollectionName;

    /**
     * 配置流式 ChatModel Bean
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            return null; // 不支持流式 Demo
        }
        
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(Duration.ofSeconds(60))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * 配置 ChatModel Bean
     * 如果没有配置 API Key，将使用占位符（启动不会失败，但调用时会报错）
     */
    @Bean
    public ChatModel chatModel() {
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            // 如果没有配置 API Key，创建一个占位实现
            return new DemoChatModel();
        }
        
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)  // 支持自定义 API 端点
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(Duration.ofSeconds(60))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * 配置对话记忆存储（内存模式）
     * 生产环境可替换为 Redis 或数据库实现
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * 配置 Embedding 模型（使用 OpenAI 兼容 API）
     * 支持 OpenAI、阿里通义、硅基流动等提供的 Embedding 服务
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            log.warn("⚠️ 未配置 API Key，Embedding 功能将不可用");
            // 返回一个简单的占位实现
            return text -> {
                throw new RuntimeException("请配置 OPENAI_API_KEY 环境变量以启用 Embedding 功能");
            };
        }
        
        log.info("🔧 初始化 Embedding 模型: {}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(embeddingModelName)
            .timeout(Duration.ofSeconds(30))
            .logRequests(true)
            .logResponses(false)
            .build();
    }

    /**
     * 配置向量存储
     * 
     * 支持两种模式：
     * - memory: 内存存储（默认，适合开发测试）
     * - chroma: Chroma 向量数据库（适合生产环境）
     * 
     * 通过 rag.vector-store.type 配置切换
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if ("chroma".equalsIgnoreCase(vectorStoreType)) {
            log.info("使用 Chroma 向量存储: {} / {}", chromaBaseUrl, chromaCollectionName);
            // Chroma v2 API (版本 >= 0.7.0 只支持 v2 API)
            return ChromaEmbeddingStore.builder()
                .apiVersion(ChromaApiVersion.V2)
                .baseUrl(chromaBaseUrl)
                .collectionName(chromaCollectionName)
                .logRequests(true)
                .logResponses(true)
                .build();
        }
        
        log.info("使用内存向量存储（重启后数据丢失）");
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Demo 实现 - 当没有配置 API Key 时使用
     * 仅用于启动测试，实际调用会提示配置 API Key
     */
    private static class DemoChatModel implements ChatModel {
        @Override
        public ChatResponse chat(ChatRequest request) {
            String responseText = "⚠️ 演示模式：请配置有效的 OPENAI_API_KEY 环境变量后重启应用。\n\n" +
                    "配置方法：\n" +
                    "export OPENAI_API_KEY=your-actual-api-key\n" +
                    "mvn spring-boot:run";
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(responseText))
                .build();
        }
    }
}
