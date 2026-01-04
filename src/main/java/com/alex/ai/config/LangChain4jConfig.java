package com.alex.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
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

    /**
     * 配置流式 ChatLanguageModel Bean
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
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
     * 配置 ChatLanguageModel Bean
     * 如果没有配置 API Key，将使用占位符（启动不会失败，但调用时会报错）
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            // 如果没有配置 API Key，创建一个占位实现
            return new DemoChatLanguageModel();
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
     * 配置向量存储（内存模式）
     * 生产环境建议使用 Pinecone、Weaviate、Milvus 等专业向量数据库
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Demo 实现 - 当没有配置 API Key 时使用
     * 仅用于启动测试，实际调用会提示配置 API Key
     */
    private static class DemoChatLanguageModel implements ChatLanguageModel {
        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            String responseText = "⚠️ 演示模式：请配置有效的 OPENAI_API_KEY 环境变量后重启应用。\n\n" +
                    "配置方法：\n" +
                    "export OPENAI_API_KEY=your-actual-api-key\n" +
                    "mvn spring-boot:run";
            return dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from(responseText)
            );
        }
    }
}
