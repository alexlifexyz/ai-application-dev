package com.alex.ai.controller;

import com.alex.ai.service.KnowledgeService;
import com.alex.ai.service.KnowledgeService.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KnowledgeController 集成测试
 * 
 * @author Alex
 * @since 2026-01-05
 */
@WebMvcTest(KnowledgeController.class)
@DisplayName("KnowledgeController 集成测试")
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeService knowledgeService;

    @Test
    @DisplayName("POST /api/knowledge - 添加知识成功")
    void addKnowledge_shouldReturnSuccess() throws Exception {
        // Given
        String title = "测试标题";
        String content = "测试内容";
        String entryId = "abc12345";
        when(knowledgeService.addKnowledge(anyString(), anyString())).thenReturn(entryId);

        // When & Then
        mockMvc.perform(post("/api/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "content", content
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.id").value(entryId))
            .andExpect(jsonPath("$.message").value("知识添加成功"));
        
        verify(knowledgeService).addKnowledge(title, content);
    }

    @Test
    @DisplayName("POST /api/knowledge - 标题为空返回错误")
    void addKnowledge_shouldReturnError_whenTitleEmpty() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "",
                    "content", "内容"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/knowledge - 获取知识列表")
    void listKnowledge_shouldReturnEntries() throws Exception {
        // Given
        KnowledgeEntry entry = new KnowledgeEntry(
            "id-001", "标题1", 100, 1, List.of(), System.currentTimeMillis()
        );
        when(knowledgeService.listKnowledge()).thenReturn(List.of(entry));

        // When & Then
        mockMvc.perform(get("/api/knowledge"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value("id-001"))
            .andExpect(jsonPath("$.data[0].title").value("标题1"))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/knowledge/{id} - 获取知识详情成功")
    void getKnowledgeDetail_shouldReturnDetail() throws Exception {
        // Given
        String id = "id-001";
        KnowledgeDetail detail = new KnowledgeDetail(
            id, "标题", 100, 1,
            List.of("片段内容"),
            System.currentTimeMillis()
        );
        when(knowledgeService.getKnowledgeDetail(anyString())).thenReturn(detail);

        // When & Then
        mockMvc.perform(get("/api/knowledge/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.title").value("标题"));
    }

    @Test
    @DisplayName("GET /api/knowledge/{id} - 知识不存在")
    void getKnowledgeDetail_shouldReturnNotFound() throws Exception {
        // Given
        when(knowledgeService.getKnowledgeDetail(anyString())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/knowledge/{id}", "non-existent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("知识条目不存在"));
    }

    @Test
    @DisplayName("DELETE /api/knowledge/{id} - 删除成功")
    void deleteKnowledge_shouldReturnSuccess() throws Exception {
        // Given
        String id = "id-to-delete";
        when(knowledgeService.deleteKnowledge(anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/knowledge/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("删除成功"));
        
        verify(knowledgeService).deleteKnowledge(id);
    }

    @Test
    @DisplayName("DELETE /api/knowledge/{id} - 删除不存在的条目")
    void deleteKnowledge_shouldReturnFalse_whenNotExists() throws Exception {
        // Given
        when(knowledgeService.deleteKnowledge(anyString())).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/knowledge/{id}", "non-existent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("知识条目不存在"));
    }

    @Test
    @DisplayName("POST /api/knowledge/search - 检索知识")
    void searchKnowledge_shouldReturnResults() throws Exception {
        // Given
        String query = "Spring Boot";
        RelevantKnowledge result = new RelevantKnowledge(
            "相关内容...", 0.85, "source-id"
        );
        when(knowledgeService.retrieveKnowledge(anyString(), anyInt()))
            .thenReturn(List.of(result));

        // When & Then
        mockMvc.perform(post("/api/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("query", query))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].score").value(0.85))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/knowledge/stats - 获取统计信息")
    void getStats_shouldReturnStats() throws Exception {
        // Given
        KnowledgeStats stats = new KnowledgeStats(5, 15, 5000L, "text-embedding-v3");
        when(knowledgeService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/knowledge/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalEntries").value(5))
            .andExpect(jsonPath("$.data.totalSegments").value(15));
    }
}
