package com.alex.ai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量嵌入服务 - 负责文本向量化和相似度搜索
 * 
 * 使用本地 all-MiniLM-L6-v2 模型，无需调用外部 API
 * 模型特点：
 * - 维度：384
 * - 多语言支持（包括中文）
 * - 轻量级，适合本地部署
 * 
 * @author Alex
 * @since 2025-01-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    
    // 使用本地嵌入模型（无需 API 调用）
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

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
        log.info("批量存储 {} 个文本片段, 来源: {}", texts.size(), source);
        
        List<TextSegment> segments = texts.stream()
            .map(text -> TextSegment.from(text, dev.langchain4j.data.document.Metadata.from("source", source)))
            .toList();
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        List<String> ids = embeddingStore.addAll(embeddings, segments);
        
        log.info("批量存储完成, 共 {} 条记录", ids.size());
        return ids;
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
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, minScore);
        
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
        return "all-MiniLM-L6-v2 (本地模型, 384维)";
    }
}
