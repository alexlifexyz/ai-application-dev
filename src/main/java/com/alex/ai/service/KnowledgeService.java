package com.alex.ai.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库服务 - 管理 RAG 知识库的增删改查
 * 
 * 改进版实现特点：
 * - 使用 ConcurrentHashMap 缓存知识条目元数据
 * - 启动时自动从 Chroma 向量库恢复已存储的知识条目
 * - 通过 EmbeddingService 进行向量化存储和检索
 * - 支持文本分段，优化检索效果
 * 
 * @author Alex
 * @since 2025-01-04
 */
@Slf4j
@Service
public class KnowledgeService {

    private final EmbeddingService embeddingService;

    /**
     * 知识条目存储（ID -> 条目信息）
     * 用于管理和展示已添加的知识
     */
    private final Map<String, KnowledgeEntry> knowledgeEntries = new ConcurrentHashMap<>();

    /**
     * 分段大小（字符数）
     */
    private static final int SEGMENT_SIZE = 500;

    /**
     * 分段重叠大小（字符数）
     */
    private static final int SEGMENT_OVERLAP = 50;

    public KnowledgeService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * 应用启动后自动从 Chroma 恢复知识条目元数据
     */
    @PostConstruct
    public void initializeFromVectorStore() {
        log.info("🔄 正在从向量库恢复知识条目元数据...");
        try {
            // 从 Chroma 获取所有存储的文档
            List<EmbeddingMatch<TextSegment>> matches = embeddingService.getAllDocuments(1000);
            
            if (matches.isEmpty()) {
                log.info("向量库为空，无需恢复");
                return;
            }
            
            // 按 source 分组统计
            Map<String, List<EmbeddingMatch<TextSegment>>> sourceGroups = new HashMap<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                String source = match.embedded().metadata().getString("source");
                if (source != null && !source.isEmpty()) {
                    sourceGroups.computeIfAbsent(source, k -> new ArrayList<>()).add(match);
                }
            }
            
            // 为每个 source 创建 KnowledgeEntry
            for (Map.Entry<String, List<EmbeddingMatch<TextSegment>>> entry : sourceGroups.entrySet()) {
                String sourceId = entry.getKey();
                List<EmbeddingMatch<TextSegment>> segments = entry.getValue();
                
                // 计算总字符数
                int totalChars = segments.stream()
                    .mapToInt(m -> m.embedded().text().length())
                    .sum();
                
                // 优先从 metadata 获取标题，否则使用内容预览
                String title = segments.get(0).embedded().metadata().getString("title");
                if (title == null || title.isEmpty()) {
                    String firstContent = segments.get(0).embedded().text();
                    title = firstContent.length() > 30 
                        ? firstContent.substring(0, 30) + "..." 
                        : firstContent;
                }
                
                // 尝试从 metadata 获取创建时间
                long createdAt = System.currentTimeMillis();
                String createdAtStr = segments.get(0).embedded().metadata().getString("createdAt");
                if (createdAtStr != null && !createdAtStr.isEmpty()) {
                    try {
                        createdAt = Long.parseLong(createdAtStr);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误，使用当前时间
                    }
                }
                
                // 创建知识条目（不包含完整的 segmentIds，因为无法完全恢复）
                KnowledgeEntry knowledgeEntry = new KnowledgeEntry(
                    sourceId,
                    title,
                    totalChars,
                    segments.size(),
                    List.of(), // segmentIds 无法完全恢复
                    createdAt
                );
                knowledgeEntries.put(sourceId, knowledgeEntry);
            }
            
            log.info("✅ 从向量库恢复了 {} 条知识条目", knowledgeEntries.size());
        } catch (Exception e) {
            log.warn("⚠️ 从向量库恢复知识条目失败: {}，将从空知识库开始", e.getMessage());
        }
    }

    /**
     * 添加知识到知识库
     * 
     * @param title 知识标题（用于显示和管理）
     * @param content 知识内容
     * @return 知识条目 ID
     */
    public String addKnowledge(String title, String content) {
        log.info("添加知识: {}, 内容长度: {} 字符", title, content.length());
        
        String entryId = UUID.randomUUID().toString().substring(0, 8);
        
        // 对长文本进行分段处理
        List<String> segments = splitText(content, SEGMENT_SIZE, SEGMENT_OVERLAP);
        log.info("文本分为 {} 个片段", segments.size());
        
        long createdAt = System.currentTimeMillis();
        
        // 存储所有分段到向量库（带标题和创建时间，便于恢复时显示）
        List<String> segmentIds = embeddingService.storeTexts(segments, entryId, title, createdAt);
        
        // 记录知识条目元数据
        KnowledgeEntry entry = new KnowledgeEntry(
            entryId, 
            title, 
            content.length(), 
            segments.size(),
            segmentIds,
            createdAt
        );
        knowledgeEntries.put(entryId, entry);
        
        log.info("知识添加成功, ID: {}", entryId);
        return entryId;
    }

    /**
     * 检索相关知识
     * 
     * @param query 查询文本
     * @param maxResults 最大返回数量
     * @return 相关知识内容列表
     */
    public List<RelevantKnowledge> retrieveKnowledge(String query, int maxResults) {
        log.info("检索知识: '{}'", query.length() > 50 ? query.substring(0, 50) + "..." : query);
        
        List<EmbeddingMatch<TextSegment>> matches = embeddingService.search(query, maxResults, 0.3);
        
        return matches.stream()
            .map(match -> new RelevantKnowledge(
                match.embedded().text(),
                match.score(),
                match.embedded().metadata().getString("source")
            ))
            .toList();
    }

    /**
     * 构建 RAG 增强的提示词
     * 
     * @param userQuery 用户原始问题
     * @return 包含相关知识的增强提示词
     */
    public String buildAugmentedPrompt(String userQuery) {
        List<RelevantKnowledge> relevantDocs = retrieveKnowledge(userQuery, 3);
        
        if (relevantDocs.isEmpty()) {
            log.info("未找到相关知识，使用原始问题");
            return userQuery;
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("请根据以下参考资料回答用户的问题。如果参考资料不足以回答问题，请基于你的知识回答，但要说明这不是来自参考资料。\n\n");
        contextBuilder.append("【参考资料】\n");
        
        for (int i = 0; i < relevantDocs.size(); i++) {
            RelevantKnowledge doc = relevantDocs.get(i);
            contextBuilder.append(String.format("[%d] (相关度: %.2f)\n%s\n\n", 
                i + 1, doc.score(), doc.content()));
        }
        
        contextBuilder.append("【用户问题】\n");
        contextBuilder.append(userQuery);
        
        log.info("构建增强提示词，包含 {} 条参考资料", relevantDocs.size());
        return contextBuilder.toString();
    }

    /**
     * 获取所有知识条目列表
     */
    public List<KnowledgeEntry> listKnowledge() {
        return new ArrayList<>(knowledgeEntries.values())
            .stream()
            .sorted((a, b) -> Long.compare(b.createdAt(), a.createdAt()))
            .toList();
    }

    /**
     * 获取知识条目详情（包括所有片段内容）
     * 
     * @param entryId 知识条目 ID
     * @return 知识详情，包含所有片段
     */
    public KnowledgeDetail getKnowledgeDetail(String entryId) {
        KnowledgeEntry entry = knowledgeEntries.get(entryId);
        if (entry == null) {
            return null;
        }
        
        // 从向量库检索该知识条目的所有片段
        List<EmbeddingMatch<TextSegment>> matches = embeddingService.getAllDocuments(1000);
        
        List<String> segments = matches.stream()
            .filter(m -> entryId.equals(m.embedded().metadata().getString("source")))
            .map(m -> m.embedded().text())
            .toList();
        
        return new KnowledgeDetail(
            entry.id(),
            entry.title(),
            entry.contentLength(),
            entry.segmentCount(),
            segments,
            entry.createdAt()
        );
    }

    /**
     * 删除知识条目
     * 注意：内存版无法真正删除向量库中的数据，仅移除元数据记录
     * 
     * @param entryId 知识条目 ID
     * @return 是否删除成功
     */
    public boolean deleteKnowledge(String entryId) {
        KnowledgeEntry removed = knowledgeEntries.remove(entryId);
        if (removed != null) {
            log.info("知识条目已删除: {} - {}", entryId, removed.title());
            // TODO: 生产环境需要实现向量库的删除操作
            return true;
        }
        return false;
    }

    /**
     * 获取知识库统计信息
     */
    public KnowledgeStats getStats() {
        int totalEntries = knowledgeEntries.size();
        int totalSegments = knowledgeEntries.values().stream()
            .mapToInt(KnowledgeEntry::segmentCount)
            .sum();
        long totalChars = knowledgeEntries.values().stream()
            .mapToLong(KnowledgeEntry::contentLength)
            .sum();
        
        return new KnowledgeStats(totalEntries, totalSegments, totalChars, embeddingService.getModelInfo());
    }

    /**
     * 文本分段算法
     * 使用滑动窗口方式，确保语义连贯性
     */
    private List<String> splitText(String text, int segmentSize, int overlap) {
        if (text.length() <= segmentSize) {
            return List.of(text);
        }
        
        List<String> segments = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + segmentSize, text.length());
            
            // 尝试在句号、换行处断开，保持语义完整
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start + segmentSize - 100, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }
            
            segments.add(text.substring(start, end).trim());
            start = end - overlap;
            
            // 防止无限循环
            if (start >= text.length() - overlap) {
                break;
            }
        }
        
        return segments;
    }

    /**
     * 查找合适的文本断点
     */
    private int findBreakPoint(String text, int searchStart, int searchEnd) {
        // 优先查找段落结束
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '.' || c == '！' || c == '?' || c == '；') {
                return i + 1;
            }
        }
        // 其次查找逗号
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            if (text.charAt(i) == '，' || text.charAt(i) == ',') {
                return i + 1;
            }
        }
        return searchEnd;
    }

    // ==================== 内部数据类 ====================

    /**
     * 知识条目记录
     */
    public record KnowledgeEntry(
        String id,
        String title,
        int contentLength,
        int segmentCount,
        List<String> segmentIds,
        long createdAt
    ) {}

    /**
     * 检索到的相关知识
     */
    public record RelevantKnowledge(
        String content,
        double score,
        String sourceId
    ) {}

    /**
     * 知识库统计信息
     */
    public record KnowledgeStats(
        int totalEntries,
        int totalSegments,
        long totalCharacters,
        String embeddingModel
    ) {}

    /**
     * 知识条目详情（包含完整片段内容）
     */
    public record KnowledgeDetail(
        String id,
        String title,
        int contentLength,
        int segmentCount,
        List<String> segments,
        long createdAt
    ) {}
}
