package com.IntelligentCustomer.system.service.document;

import java.io.File;

import java.util.List;

import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;
    private final MilvusVectorRepository milvusVectorRepository;
    
    public DocumentProcessingService(DocumentService documentService, 
                                     EmbeddingService embeddingService, 
                                     MilvusVectorRepository milvusVectorRepository) {
        this.documentService = documentService;
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
    }

    public void processDocument(String filepath, String filename) {
        File file = new File(filepath);
        
        try {
            // 文件切分
            List<DocumentChunk> documents = documentService.parseAndSplit(
                    filename,
                    file,
                    filename
            );

            // 嵌入向量
            List<KnowledgeVector> vectors = embeddingService.embedDocuments(documents, filename);

            // 保存向量到数据库
            milvusVectorRepository.save(vectors);
            
            // 处理完成，删除临时文件
            if (file.exists()) {
                file.delete();
            }
            
            logger.info("文档处理完成: filename={}, chunks={}, vectors={}", 
                    filename, documents.size(), vectors.size());

        } catch (Exception e) {
            logger.error("处理文档失败: filepath={}, error={}", filepath, e.getMessage(), e);
            throw new BusinessException("文档处理失败", e);
        }
    }
}
