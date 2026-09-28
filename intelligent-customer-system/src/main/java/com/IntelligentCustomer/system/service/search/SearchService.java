package com.IntelligentCustomer.system.service.search;

import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final EmbeddingService embeddingService;
    private final MilvusVectorRepository milvusVectorRepository;

    public SearchService(
            EmbeddingService embeddingService,
            MilvusVectorRepository milvusVectorRepository) {
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
    }

    public List<KnowledgeVector> semanticSearch(String query, int topK) {
        float[] queryVector = embeddingService.embed(query);
        return milvusVectorRepository.search(queryVector, topK);
    }

    // MySQL FULLTEXT 关键词检索将在下一阶段接入。
    public List<KnowledgeVector> hybridSearch(String query, int topK) {
        return semanticSearch(query, topK);
    }
}
