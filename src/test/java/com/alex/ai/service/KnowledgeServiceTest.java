package com.alex.ai.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeService 单元测试
 * 
 * @author Alex
 * @since 2026-01-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeService 单元测试")
class KnowledgeServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(embeddingService);
    }

    @Test
    @DisplayName("添加知识 - 短文本不分段")
    void addKnowledge_shouldNotSplitShortText() {
        // Given
        String title = "测试标题";
        String content = "这是一段简短的测试内容";
        when(embeddingService.storeTexts(anyList(), anyString(), anyString(), anyLong()))
            .thenReturn(List.of("id-001"));

        // When
        String entryId = knowledgeService.addKnowledge(title, content);

        // Then
        assertThat(entryId).isNotNull();
        assertThat(entryId).hasSize(8);
        verify(embeddingService).storeTexts(anyList(), anyString(), eq(title), anyLong());
    }

    @Test
    @DisplayName("添加知识 - 长文本自动分段")
    void addKnowledge_shouldSplitLongText() {
        // Given
        String title = "长文本测试";
        // 创建超过 500 字符的内容
        String content = "这是一段很长的测试内容。".repeat(100);
        when(embeddingService.storeTexts(anyList(), anyString(), anyString(), anyLong()))
            .thenReturn(List.of("id-001", "id-002", "id-003"));

        // When
        String entryId = knowledgeService.addKnowledge(title, content);

        // Then
        assertThat(entryId).isNotNull();
        // 验证传递给 embeddingService 的文本列表包含多个分段
        verify(embeddingService).storeTexts(
            argThat(list -> list.size() > 1),
            anyString(),
            eq(title),
            anyLong()
        );
    }

    @Test
    @DisplayName("获取知识列表")
    void listKnowledge_shouldReturnAllEntries() {
        // Given - 先添加一些知识
        when(embeddingService.storeTexts(anyList(), anyString(), anyString(), anyLong()))
            .thenReturn(List.of("id-001"));
        
        knowledgeService.addKnowledge("标题1", "内容1");
        knowledgeService.addKnowledge("标题2", "内容2");

        // When
        var entries = knowledgeService.listKnowledge();

        // Then
        assertThat(entries).hasSize(2);
    }

    @Test
    @DisplayName("删除知识 - 存在的条目")
    void deleteKnowledge_shouldRemoveExistingEntry() {
        // Given
        when(embeddingService.storeTexts(anyList(), anyString(), anyString(), anyLong()))
            .thenReturn(List.of("segment-001"));
        
        String entryId = knowledgeService.addKnowledge("待删除", "测试内容");

        // When
        boolean deleted = knowledgeService.deleteKnowledge(entryId);

        // Then
        assertThat(deleted).isTrue();
        assertThat(knowledgeService.listKnowledge()).isEmpty();
    }

    @Test
    @DisplayName("删除知识 - 不存在的条目")
    void deleteKnowledge_shouldReturnFalseForNonExistent() {
        // When
        boolean deleted = knowledgeService.deleteKnowledge("non-existent-id");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("检索知识 - 有相关结果")
    void retrieveKnowledge_shouldReturnRelevantResults() {
        // Given
        String query = "测试查询";
        TextSegment segment = TextSegment.from("相关内容", Metadata.from("source", "test-id"));
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "embed-id", null, segment);
        when(embeddingService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(List.of(match));

        // When
        var results = knowledgeService.retrieveKnowledge(query, 5);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).isEqualTo("相关内容");
        assertThat(results.get(0).score()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("检索知识 - 无相关结果")
    void retrieveKnowledge_shouldReturnEmptyWhenNoMatch() {
        // Given
        when(embeddingService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(List.of());

        // When
        var results = knowledgeService.retrieveKnowledge("无关查询", 5);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("构建增强提示词 - 有相关知识")
    void buildAugmentedPrompt_shouldIncludeRelevantKnowledge() {
        // Given
        String query = "什么是 Spring Boot？";
        TextSegment segment = TextSegment.from(
            "Spring Boot 是一个简化 Spring 应用开发的框架",
            Metadata.from("source", "test-id")
        );
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "embed-id", null, segment);
        when(embeddingService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(List.of(match));

        // When
        String augmented = knowledgeService.buildAugmentedPrompt(query);

        // Then
        assertThat(augmented).contains("参考资料");
        assertThat(augmented).contains("Spring Boot");
        assertThat(augmented).contains(query);
    }

    @Test
    @DisplayName("构建增强提示词 - 无相关知识时返回原始查询")
    void buildAugmentedPrompt_shouldReturnOriginalWhenNoKnowledge() {
        // Given
        String query = "随便说点什么";
        when(embeddingService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(List.of());

        // When
        String augmented = knowledgeService.buildAugmentedPrompt(query);

        // Then
        assertThat(augmented).isEqualTo(query);
    }

    @Test
    @DisplayName("获取统计信息")
    void getStats_shouldReturnCorrectStats() {
        // Given
        when(embeddingService.storeTexts(anyList(), anyString(), anyString(), anyLong()))
            .thenReturn(List.of("id-001", "id-002"));
        when(embeddingService.getModelInfo()).thenReturn("text-embedding-v3");
        
        knowledgeService.addKnowledge("标题", "内容".repeat(200)); // 触发分段

        // When
        var stats = knowledgeService.getStats();

        // Then
        assertThat(stats.totalEntries()).isEqualTo(1);
        assertThat(stats.totalSegments()).isGreaterThanOrEqualTo(1);
    }
}
