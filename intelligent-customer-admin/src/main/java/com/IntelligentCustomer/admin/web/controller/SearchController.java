package com.IntelligentCustomer.admin.web.controller;

import com.IntelligentCustomer.system.domain.dto.search.HybridSearchResult;
import com.IntelligentCustomer.system.domain.dto.search.SearchRequest;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.service.search.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 语义检索接口
     */
    @PostMapping("/semantic")
    public ResponseEntity<?> semanticSearch(
            @Valid @RequestBody SearchRequest request
    ) {
        List<KnowledgeVector> results =
                searchService.semanticSearch(request.getQuery(), request.getTopK());

        return ResponseEntity.ok(
                response(
                        results.isEmpty() ? "未找到相关知识" : "语义搜索成功",
                        results
                )
        );
    }

    /**
     * 混合检索接口
     */
    @PostMapping("/hybrid")
    public ResponseEntity<?> hybridSearch(
            @Valid @RequestBody SearchRequest request
    ) {
        List<HybridSearchResult> results =
                searchService.hybridSearch(request.getQuery(), request.getTopK());

        return ResponseEntity.ok(
                response(
                        results.isEmpty() ? "未找到相关知识" : "混合搜索成功",
                        results
                )
        );
    }

    /**
     * 统一构造响应结果
     *
     * List<?> 可以同时接收：
     * List<KnowledgeVector>
     * List<HybridSearchResult>
     */
    private Map<String, Object> response(
            String message,
            List<?> results
    ) {
        Map<String, Object> response = new HashMap<>();

        response.put("code", 200);
        response.put("message", message);
        response.put("data", results);
        response.put("totalCount", results.size());

        return response;
    }
}
