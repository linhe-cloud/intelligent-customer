package com.IntelligentCustomer.admin.web.controller;
import java.util.List;

import com.IntelligentCustomer.system.domain.dto.EmbedRequest;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/embedding")
public class EmbeddingController {
    private final EmbeddingService embeddingService;
    private final MilvusVectorRepository milvusVectorRepository;

    public EmbeddingController(EmbeddingService embeddingService, MilvusVectorRepository milvusVectorRepository) {
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
    }

    // 用户提问
    @PostMapping("/embed")
    public float[] embed(@RequestBody EmbedRequest request) {
        return embeddingService.embed(request.getText());
    }

    // 搜索
    @PostMapping("/search")
    public List<KnowledgeVector> search(@RequestBody EmbedRequest request) {
        float[] vector = embeddingService.embed(request.getText());
        return milvusVectorRepository.search(vector, 3);
    }
}
