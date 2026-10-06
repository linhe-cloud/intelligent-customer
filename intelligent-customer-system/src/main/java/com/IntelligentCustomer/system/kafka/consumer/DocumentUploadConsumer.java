package com.IntelligentCustomer.system.kafka.consumer;

import com.IntelligentCustomer.system.repository.mapper.KnowledgeChunkMapper;
import com.IntelligentCustomer.system.service.document.DocumentService;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.domain.dto.kafka.DocumentUploadMessage;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;
import com.IntelligentCustomer.system.domain.entity.KnowledgeChunk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 文档上传消息消费者服务
 * 负责消费Kafka中的文档上传消息，处理文档并生成向量存储到Milvus
 */
@Service
public class DocumentUploadConsumer {
    
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(DocumentUploadConsumer.class);

    // 依赖注入的各个服务
    private final DocumentService documentService;      // 文档处理服务
    private final EmbeddingService embeddingService;    // 向量嵌入服务
    private final MilvusVectorRepository milvusVectorRepository;  // Milvus向量存储库
    private final ObjectMapper objectMapper;            // JSON对象映射器
    private final FileProcessingRecordMapper fileProcessingRecordMapper;  // 文件处理记录映射器
    private final KnowledgeChunkMapper knowledgeChunkMapper;  // 知识块映射器

    /**
     * 构造函数，注入所需的服务
     * @param documentService 文档处理服务
     * @param embeddingService 向量嵌入服务
     * @param milvusVectorRepository Milvus向量存储库
     * @param objectMapper JSON对象映射器
     * @param fileProcessingRecordMapper 文件处理记录映射器
     * @param knowledgeChunkMapper 知识块映射器
     */
    public DocumentUploadConsumer(DocumentService documentService, EmbeddingService embeddingService, MilvusVectorRepository milvusVectorRepository, ObjectMapper objectMapper, FileProcessingRecordMapper fileProcessingRecordMapper, KnowledgeChunkMapper knowledgeChunkMapper) {
        this.documentService = documentService;
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
        this.objectMapper = objectMapper;
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }
    
    /**
     * 消费Kafka消息的处理方法
     * @param message 接收到的Kafka消息内容
     */
    @KafkaListener(topics = "document-upload", groupId = "document-processor-group")
    public void consume(String message) {
    
        DocumentUploadMessage uploadMsg = null;

        try {
            // 将JSON消息转换为DocumentUploadMessage对象
            uploadMsg = objectMapper.readValue(message, DocumentUploadMessage.class);

            // 幂等保护：检查文档当前状态
            FileProcessingRecord record = fileProcessingRecordMapper.findByFileId(uploadMsg.getFileId());
            if (record != null) {
                // 如果文档已处理成功，跳过重复消费
                if ("SUCCESS".equals(record.getStatus())) {
                    log.info("文档已处理成功，跳过重复消费: fileId={}", uploadMsg.getFileId());
                    return;
                }

                log.warn("文档尚未成功，清除旧数据后重新处理：fileId={}", uploadMsg.getFileId());

                // 如果不是 SUCCESS 就清除旧数据
                milvusVectorRepository.deleteByFileId(uploadMsg.getFileId());
                knowledgeChunkMapper.deleteByFileId(uploadMsg.getFileId());
            }

            // 更新为处理中
            fileProcessingRecordMapper.updateProcessing(uploadMsg.getFileId());

            // 执行处理，先拆分文件，再生成向量
            File file = new File(uploadMsg.getFilePath());
            List<DocumentChunk> documents = documentService.parseAndSplit(
                    uploadMsg.getFileId(),
                    file,
                    uploadMsg.getFileName()
            );
            List<KnowledgeVector> vectors = embeddingService.embedDocuments(
                    documents,
                    uploadMsg.getFileId(),
                    uploadMsg.getFileName()
            );

            List<KnowledgeChunk> chunks = new ArrayList<>(vectors.size());

            for (KnowledgeVector vector : vectors) {
                KnowledgeChunk chunk = new KnowledgeChunk();

                // MySQL 和 Milvus 使用同一个ID
                chunk.setId(vector.getId());
                chunk.setFileId(vector.getFileId());
                chunk.setContent(vector.getContent());
                chunk.setSourceFile(vector.getSourceFile());
                chunk.setChunkIndex(vector.getChunkIndex());
                chunk.setCreatedAt(vector.getCreatedAt());

                chunks.add(chunk);
            }

            // 先保存文本切片到 MySQL
            for (KnowledgeChunk chunk : chunks) {
                knowledgeChunkMapper.insert(chunk);
            }

            // 保存文档分块向量到 Milvus
            milvusVectorRepository.save(vectors);

            // 更新为成功
            fileProcessingRecordMapper.updateSuccess(uploadMsg.getFileId(), documents.size(), vectors.size());

            log.info("文档处理完成：fileId={}, fileName={}, chunks={}, vectors={}", uploadMsg.getFileId(), uploadMsg.getFileName(), documents.size(), vectors.size());
        } catch (Exception e) {
            log.error("文档处理失败：{}", e.getMessage(), e);
            if (uploadMsg != null) {
                try {
                    milvusVectorRepository.deleteByFileId(uploadMsg.getFileId());
                    knowledgeChunkMapper.deleteByFileId(uploadMsg.getFileId());
                } catch (Exception cleanupException) {
                    log.error("清理文档处理残留数据失败: fileId={}",
                            uploadMsg.getFileId(),
                            cleanupException);
                }

                fileProcessingRecordMapper.updateFailure(
                        uploadMsg.getFileId(),
                        e.getMessage()
                );
            }
        }
    }
}
