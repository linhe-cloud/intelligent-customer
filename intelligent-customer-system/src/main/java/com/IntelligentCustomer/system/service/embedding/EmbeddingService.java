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

/**
 * 嵌入服务类
 * 提供文本向量化的核心功能，支持单条查询和批量处理
 */
@Service
public class EmbeddingService {
    // BGE嵌入客户端，用于文本向量化
    private final BgeEmbeddingClient bgeEmbeddingClient;

    /**
     * 构造函数
     * @param bgeEmbeddingClient BGE嵌入客户端，用于文本向量化
     */
    public EmbeddingService(BgeEmbeddingClient bgeEmbeddingClient) {
        this.bgeEmbeddingClient = bgeEmbeddingClient;
    }

    // ========= 单条查询 =========
    /**
     * 单条文本转向量 - 用户提问使用
     * 该方法将输入的文本转换为向量表示，用于后续的语义相似度计算或其他向量操作
     * @param text 需要转换为向量的文本内容
     * @return float[] 返回文本对应的向量表示，一个浮点数数组
     * @throws BusinessException 当输入文本为null或空字符串时抛出异常
     */
    public float[] embed(String text) {
    // 检查输入文本是否为null或空字符串（去除首尾空格后）
        if (text == null || text.trim().isEmpty()) {
        // 如果文本无效，抛出业务异常
            throw new BusinessException("文本不能为空");
        }
    // 调用BGE嵌入客户端将文本转换为向量，并返回第一个结果
        return bgeEmbeddingClient.embed(List.of(text)).get(0);
    }

    // ========= 批量入库 =========
    /**
     * 批量文本转向量 - 入库使用
     * 该方法用于将文本列表批量转换为向量表示，主要用于文本入库前的预处理
     *
     * @param texts 需要转换为向量的文本列表
     * @return 返回向量列表，每个向量对应一个输入文本，如果输入为空则返回空列表
     */
    public List<float[]> embedBatch(List<String> texts) {
    // 检查输入文本列表是否为空或null，如果是则返回空列表
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
    // 调用BGE嵌入客户端的embed方法，将文本列表批量转换为向量
        return bgeEmbeddingClient.embed(texts);
    }

    /**
     * 文档批量处理 - 封装完整入库流程
     * 该方法将文档块列表转换为知识向量列表，包含完整的文本提取、向量化封装过程
     * @param documents 文档块列表，包含需要处理的文档内容
     * @param fileId 文件ID，标识文档来源
     * @param filename 文件名，标识文档来源
     * @return 返回处理后的知识向量列表
     */
    public List<KnowledgeVector> embedDocuments(
            List<DocumentChunk> documents,    // 文档块列表，每个块包含文档内容
            String fileId,                    // 文件唯一标识符
            String filename                   // 文件名称
    ) {
    // 参数校验：检查文档列表是否为空
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }
    // 参数校验：检查文件ID是否为空
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new BusinessException("文件ID不能为空");
        }
    // 参数校验：检查文件名是否为空
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
            kv.setFileId(fileId);
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
