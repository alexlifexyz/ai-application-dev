package com.alex.ai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量嵌入服务 - 负责文本向量化和相似度搜索
 * 
 * 使用远程 Embedding API（OpenAI/通义千问等兼容接口）
 * 
 * @author Alex
 * @since 2025-01-04
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final String modelName;
    private final String baseUrl;

    /**
     * 构造函数注入（从 Spring 容器获取已配置的 Bean）
     */
    public EmbeddingService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-v3}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        log.info("✅ EmbeddingService 初始化成功");
        log.info("   - Embedding 模型: {}", embeddingModel.getClass().getSimpleName());
        log.info("   - 向量存储: {}", embeddingStore.getClass().getSimpleName());
    }

    /**
     * 将文本存储到向量库
     * 
     * @param text 文本内容
     * @param metadata 元数据标识（用于后续删除/查询）
     * @return 存储的文档 ID
     */
    public String storeText(String text, String metadata) {
        log.info("存储文本到向量库, 元数据: {}, 长度: {} 字符", metadata, text.length());
        
        TextSegment segment = TextSegment.from(text, dev.langchain4j.data.document.Metadata.from("source", metadata));
        Embedding embedding = embeddingModel.embed(segment).content();
        
        String id = embeddingStore.add(embedding, segment);
        log.info("文本已存储, ID: {}", id);
        return id;
    }

    /**
     * 批量存储文本片段
     * 
     * @param texts 文本列表
     * @param source 来源标识
     * @return 存储的文档 ID 列表
     */
    public List<String> storeTexts(List<String> texts, String source) {
        return storeTexts(texts, source, null);
    }

    /**
     * 批量存储文本片段（带标题和创建时间）
     * 
     * @param texts 文本列表
     * @param source 来源标识
     * @param title 知识条目标题（用于恢复时显示）
     * @param createdAt 创建时间戳
     * @return 存储的文档 ID 列表
     */
    public List<String> storeTexts(List<String> texts, String source, String title, long createdAt) {
        log.info("批量存储 {} 个文本片段, 来源: {}, 标题: {}", texts.size(), source, title);
        
        List<TextSegment> segments = texts.stream()
            .map(text -> {
                var metadata = dev.langchain4j.data.document.Metadata.from("source", source);
                if (title != null && !title.isEmpty()) {
                    metadata.put("title", title);
                }
                metadata.put("createdAt", String.valueOf(createdAt));
                return TextSegment.from(text, metadata);
            })
            .toList();
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        List<String> ids = embeddingStore.addAll(embeddings, segments);
        
        log.info("批量存储完成, 共 {} 条记录", ids.size());
        return ids;
    }

    /**
     * 批量存储文本片段（带标题）
     * 
     * @param texts 文本列表
     * @param source 来源标识
     * @param title 知识条目标题（用于恢复时显示）
     * @return 存储的文档 ID 列表
     */
    public List<String> storeTexts(List<String> texts, String source, String title) {
        return storeTexts(texts, source, title, System.currentTimeMillis());
    }

    /**
     * 获取向量库中所有存储的文档（用于启动时恢复）
     * 使用一个通用查询词来获取所有文档
     * 
     * @param maxResults 最大返回数量
     * @return 所有匹配的文档
     */
    public List<EmbeddingMatch<TextSegment>> getAllDocuments(int maxResults) {
        log.info("获取向量库中所有文档, 最大数量: {}", maxResults);
        
        try {
            // 使用一个通用的查询词来获取文档，设置最低阈值
            Embedding queryEmbedding = embeddingModel.embed("知识 内容 文档 信息").content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.0) // 最低阈值，获取所有可能的文档
                .build();
            
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();
            
            log.info("获取到 {} 个文档", matches.size());
            return matches;
        } catch (Exception e) {
            log.warn("获取所有文档失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 相似度搜索 - 查找与查询最相关的文本
     * 
     * @param query 查询文本
     * @param maxResults 最大返回数量
     * @param minScore 最小相似度阈值 (0-1)
     * @return 匹配的文本片段列表
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults, double minScore) {
        log.info("执行相似度搜索: '{}', maxResults={}, minScore={}", 
            query.length() > 50 ? query.substring(0, 50) + "..." : query, 
            maxResults, minScore);
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // LangChain4j 1.x: 使用 EmbeddingSearchRequest 和 search() 方法
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();
        
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        
        log.info("搜索完成, 找到 {} 个相关文档", matches.size());
        return matches;
    }

    /**
     * 简化的相似度搜索（使用默认参数）
     * 
     * @param query 查询文本
     * @return 前 3 个最相关的文本片段
     */
    public List<EmbeddingMatch<TextSegment>> search(String query) {
        return search(query, 3, 0.5);
    }

    /**
     * 获取嵌入模型信息
     */
    public String getModelInfo() {
        // 判断 API 提供商
        String provider = "远程 API";
        if (baseUrl.contains("dashscope.aliyuncs.com")) {
            provider = "通义千问";
        } else if (baseUrl.contains("api.openai.com")) {
            provider = "OpenAI";
        } else if (baseUrl.contains("api.siliconflow.cn")) {
            provider = "硅基流动";
        }
        
        // 根据模型名称判断向量维度
        int dimension = 1024; // text-embedding-v3 默认维度
        if (modelName.contains("text-embedding-3-small")) {
            dimension = 1536;
        } else if (modelName.contains("text-embedding-3-large")) {
            dimension = 3072;
        } else if (modelName.contains("text-embedding-ada-002")) {
            dimension = 1536;
        }
        
        return String.format("%s (%s, %d维)", modelName, provider, dimension);
    }
}
