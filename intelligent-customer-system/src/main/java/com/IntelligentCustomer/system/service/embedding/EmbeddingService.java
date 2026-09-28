package com.IntelligentCustomer.system.service.embedding;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.framework.client.bge.BgeEmbeddingClient;
import org.springframework.stereotype.Service;

// 调用ai模型，把文本转成向量
@Service
public class EmbeddingService {
    private final BgeEmbeddingClient bgeEmbeddingClient;

    public EmbeddingService(BgeEmbeddingClient bgeEmbeddingClient) {
        this.bgeEmbeddingClient = bgeEmbeddingClient;
    }

    // ========= 单条查询 =========
    /**
     * 单条文本转向量 - 用户提问使用
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException("文本不能为空");
        }
        return bgeEmbeddingClient.embed(List.of(text)).get(0);
    }

    // ========= 批量入库 =========
    /**
     * 批量文本转向量 - 入库使用
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        return bgeEmbeddingClient.embed(texts);
    }

    /**
     * 文档批量处理 - 封装完整入库流程
     */
    public List<KnowledgeVector> embedDocuments(List<DocumentChunk> documents, String filename) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 提取文本
        List<String> texts = documents.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());

        // 批量转成向量
        List<float[]> embeddings = embedBatch(texts);

        // 封装结果
        List<KnowledgeVector> result = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeVector kv = new KnowledgeVector();
            kv.setId(UUID.randomUUID());
            kv.setContent(documents.get(i).getContent());
            kv.setEmbedding(embeddings.get(i));
            kv.setSourceFile(filename);
            kv.setChunkIndex(i);
            kv.setCreatedAt(LocalDateTime.now());
            result.add(kv);
        }
        return result;
    }
}
