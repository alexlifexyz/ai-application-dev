package com.alex.ai.controller;

import com.alex.ai.service.KnowledgeService;
import com.alex.ai.service.KnowledgeService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 * 
 * 提供知识库的增删改查 REST API
 * 用于 RAG 功能的知识管理
 * 
 * @author Alex
 * @since 2025-01-04
 */
@Tag(name = "知识库管理", description = "RAG 知识库管理接口")
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 添加知识
     */
    @Operation(summary = "添加知识", description = "将文本内容添加到知识库，支持长文本自动分段")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addKnowledge(@Valid @RequestBody AddKnowledgeRequest request) {
        log.info("添加知识请求: {}", request.getTitle());
        
        String entryId = knowledgeService.addKnowledge(request.getTitle(), request.getContent());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", entryId);
        response.put("message", "知识添加成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识列表
     */
    @Operation(summary = "获取知识列表", description = "获取所有已添加的知识条目")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listKnowledge() {
        List<KnowledgeEntry> entries = knowledgeService.listKnowledge();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", entries);
        response.put("total", entries.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识详情
     */
    @Operation(summary = "获取知识详情", description = "根据 ID 获取知识条目的完整内容")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKnowledgeDetail(@PathVariable String id) {
        log.info("获取知识详情: {}", id);
        
        var detail = knowledgeService.getKnowledgeDetail(id);
        
        Map<String, Object> response = new HashMap<>();
        if (detail != null) {
            response.put("success", true);
            response.put("data", detail);
        } else {
            response.put("success", false);
            response.put("message", "知识条目不存在");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除知识
     */
    @Operation(summary = "删除知识", description = "根据 ID 删除知识条目")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledge(@PathVariable String id) {
        log.info("删除知识请求: {}", id);
        
        boolean deleted = knowledgeService.deleteKnowledge(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", deleted);
        response.put("message", deleted ? "删除成功" : "知识条目不存在");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检索相关知识
     */
    @Operation(summary = "检索知识", description = "根据查询文本检索相关知识片段")
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(@Valid @RequestBody SearchRequest request) {
        log.info("检索知识请求: {}", request.getQuery());
        
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 5;
        List<RelevantKnowledge> results = knowledgeService.retrieveKnowledge(request.getQuery(), maxResults);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results);
        response.put("total", results.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识库统计信息
     */
    @Operation(summary = "知识库统计", description = "获取知识库的统计信息")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        KnowledgeStats stats = knowledgeService.getStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);
        
        return ResponseEntity.ok(response);
    }

    // ==================== 请求 DTO ====================

    @Data
    public static class AddKnowledgeRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        
        @NotBlank(message = "内容不能为空")
        private String content;
    }

    @Data
    public static class SearchRequest {
        @NotBlank(message = "查询内容不能为空")
        private String query;
        
        private Integer maxResults;
    }
}
