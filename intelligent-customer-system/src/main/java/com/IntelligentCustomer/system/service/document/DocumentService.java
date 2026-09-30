package com.IntelligentCustomer.system.service.document;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import com.IntelligentCustomer.system.service.document.parser.DocumentInput;
import com.IntelligentCustomer.system.service.document.parser.DocumentProcessor;
import com.IntelligentCustomer.system.service.document.parser.DocumentProcessorFactory;
import com.IntelligentCustomer.system.service.document.parser.FileDocumentInput;
import com.IntelligentCustomer.system.service.document.transform.TextCleaner;
import com.IntelligentCustomer.system.service.document.transform.TextSplitter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 文档服务类，负责处理文档的解析、清理和分割操作
 */
@Service
public class DocumentService {

    /**
     * 文档处理器工厂，用于根据文件类型创建相应的处理器
     */
    private final DocumentProcessorFactory processorFactory;
    /**
     * 文本清理器，用于清理文档中的无关内容
     */
    private final TextCleaner textCleaner;
    /**
     * 文本分割器，用于将文档分割成小块
     */
    private final TextSplitter textSplitter;
    /**
     * 文件处理记录映射器，用于操作文件处理记录的数据库表
     */
    private final FileProcessingRecordMapper fileProcessingRecordMapper;
    /**
     * 文件存储服务，用于处理文件的上传和下载
     */
    private final FileStorageService fileStorageService;
    /**
     * Milvus向量存储库，用于存储文档的向量表示
     */
    private final MilvusVectorRepository milvusVectorRepository;

    /**
     * 构造函数，通过依赖注入的方式初始化所需的服务组件
     *
     * @param processorFactory 文档处理器工厂
     * @param textCleaner      文本清理器
     * @param textSplitter     文本分割器
     */
    public DocumentService(
            DocumentProcessorFactory processorFactory,
            TextCleaner textCleaner,
            TextSplitter textSplitter,
            FileProcessingRecordMapper fileProcessingRecordMapper,
            FileStorageService fileStorageService,
            MilvusVectorRepository milvusVectorRepository
    ) {
        this.processorFactory = processorFactory;
        this.textCleaner = textCleaner;
        this.textSplitter = textSplitter;
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
        this.fileStorageService = fileStorageService;
        this.milvusVectorRepository = milvusVectorRepository;
    }

    /**
     * 解析并分割文档的主要方法
     *
     * @param fileId           文件ID
     * @param file             文件对象
     * @param originalFilename 原始文件名
     * @return 分割后的文档块列表
     */
    public List<DocumentChunk> parseAndSplit(
            String fileId,
            File file,
            String originalFilename
    ) {
        // 创建文档输入对象
        DocumentInput input =
                new FileDocumentInput(file, originalFilename);

        // 根据文件类型获取相应的处理器
        DocumentProcessor processor =
                processorFactory.getProcessor(originalFilename);

        // 解析文档
        ParsedDocument parsedDocument = processor.parse(input);

        // 清理每个文档节点的文本内容
        for (ParsedSection section : parsedDocument.getSections()) {
            section.setContent(
                    textCleaner.clean(section.getContent())
            );
        }

        // 移除内容为空的文档节点
        parsedDocument.getSections().removeIf(section ->
                section.getContent() == null
                        || section.getContent().isBlank()
        );

        // 如果清理后没有有效内容，抛出业务异常
        if (parsedDocument.getSections().isEmpty()) {
            throw new BusinessException("文档没有可入库的有效文本");
        }

        // 将文档分割成小块并返回
        return textSplitter.split(fileId, parsedDocument);
    }


    public Map<String, Object> deleteDocument(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException("文件ID不能为空");
        }

        FileProcessingRecord record =
                fileProcessingRecordMapper.findByFileId(fileId);

        if (record == null) {
            throw new BusinessException("文档不存在");
        }

        // 先删除 Milvus 中该文件对应的向量
        milvusVectorRepository.deleteByFileId(fileId);

        // 再删除本地文件
        fileStorageService.delete(record.getFilePath());

        // 最后删除 MySQL 记录
        int deleted = fileProcessingRecordMapper.deleteByFileId(fileId);

        if (deleted == 0) {
            throw new BusinessException("删除文档记录失败");
        }

        return Map.of(
                "code", 200,
                "fileId", fileId,
                "message", "文档删除成功"
        );
    }
}