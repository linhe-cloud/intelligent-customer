package com.IntelligentCustomer.admin.web.controller;

import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.service.search.SearchService;
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

    @PostMapping("/semantic")
    public ResponseEntity<?> semanticSearch(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.get("topk") == null
                ? 5
                : ((Number) request.get("topk")).intValue();

        List<KnowledgeVector> results = searchService.semanticSearch(query, topK);
        return ResponseEntity.ok(response("语义搜索成功", results));
    }

    @PostMapping("/hybrid")
    public ResponseEntity<?> hybridSearch(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.get("topk") == null
                ? 5
                : ((Number) request.get("topk")).intValue();

        List<KnowledgeVector> results = searchService.hybridSearch(query, topK);
        return ResponseEntity.ok(response("搜索成功", results));
    }

    private Map<String, Object> response(String message, List<KnowledgeVector> results) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", message);
        response.put("data", results);
        response.put("totalCount", results.size());
        return response;
    }
}
