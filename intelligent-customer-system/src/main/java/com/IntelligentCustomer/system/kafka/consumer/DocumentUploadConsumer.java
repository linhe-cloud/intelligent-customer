package com.IntelligentCustomer.system.kafka.consumer;

import com.IntelligentCustomer.system.service.document.DocumentService;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.domain.dto.kafka.DocumentUploadMessage;
import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class DocumentUploadConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentUploadConsumer.class);

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;
    private final MilvusVectorRepository milvusVectorRepository;
    private final ObjectMapper objectMapper;
    private final FileProcessingRecordMapper fileProcessingRecordMapper;

    public DocumentUploadConsumer(DocumentService documentService, EmbeddingService embeddingService, MilvusVectorRepository milvusVectorRepository, ObjectMapper objectMapper, FileProcessingRecordMapper fileProcessingRecordMapper) {
        this.documentService = documentService;
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
        this.objectMapper = objectMapper;
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
    }
    
    @KafkaListener(topics = "document-upload", groupId = "document-processor-group")
    public void consume(String message) {
    
        DocumentUploadMessage uploadMsg = null;

        try {
            uploadMsg = objectMapper.readValue(message, DocumentUploadMessage.class);

            // 幂等保护：检查文档当前状态
            FileProcessingRecord record = fileProcessingRecordMapper.findByFileId(uploadMsg.getFileId());
            if (record != null) {
                if ("SUCCESS".equals(record.getStatus())) {
                    log.info("文档已处理成功，跳过重复消费: fileId={}", uploadMsg.getFileId());
                    return;
                }
                // 如果是 PROCESSING 状态（上次处理中途崩溃），先清理已有数据再重新处理
                if ("PROCESSING".equals(record.getStatus())) {
                    log.warn("文档处于PROCESSING状态，清理旧数据后重新处理: fileId={}", uploadMsg.getFileId());
                    milvusVectorRepository.deleteBySourceFile(uploadMsg.getFileName());
                }
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
            List<KnowledgeVector> vectors = embeddingService.embedDocuments(documents, uploadMsg.getFileName());

            // 保存文档分块向量到 Milvus
            milvusVectorRepository.save(vectors);

            // 更新为成功
            fileProcessingRecordMapper.updateSuccess(uploadMsg.getFileId(), documents.size(), vectors.size());

            // 删除临时文件
            file.delete();

            log.info("文档处理完成：fileId={}, fileName={}, chunks={}, vectors={}", uploadMsg.getFileId(), uploadMsg.getFileName(), documents.size(), vectors.size());
        } catch (Exception e) {
            log.error("文档处理失败：{}", e.getMessage(), e);
            if (uploadMsg != null) {
                fileProcessingRecordMapper.updateFailure(uploadMsg.getFileId(), e.getMessage());
            }
            throw new BusinessException("文档处理失败", e);
        }
    }
}
