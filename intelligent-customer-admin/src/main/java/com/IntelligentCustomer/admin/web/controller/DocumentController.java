package com.IntelligentCustomer.admin.web.controller;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.dto.kafka.DocumentUploadMessage;
import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.kafka.producer.DocumentUploadProducer;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import com.IntelligentCustomer.system.service.document.DocumentService;
import com.IntelligentCustomer.system.service.document.FileStorageService;
import com.IntelligentCustomer.system.service.document.FileValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档控制器，提供文档上传、删除、列表查询和重试等功能
 */
@RestController
@RequestMapping("/document")
public class DocumentController {

    /**
     * 文件处理记录数据访问层
     */
    private final FileProcessingRecordMapper fileProcessingRecordMapper;
    /**
     * 文档服务层
     */
    private final DocumentService documentService;
    /**
     * 文档上传消息生产者
     */
    private final DocumentUploadProducer documentUploadProducer;
    /**
     * Milvus向量仓库
     */
    private final MilvusVectorRepository milvusVectorRepository;
    /**
     * 文件验证服务
     */
    private final FileValidationService fileValidationService;
    /**
     * 文件存储服务
     */
    private final FileStorageService fileStorageService;

    /**
     * 构造函数，注入所需的服务
     * @param fileProcessingRecordMapper 文件处理记录数据访问层
     * @param documentService 文档服务层
     * @param documentUploadProducer 文档上传消息生产者
     * @param milvusVectorRepository Milvus向量仓库
     * @param fileValidationService 文件验证服务
     * @param fileStorageService 文件存储服务
     */
    public DocumentController(
            FileProcessingRecordMapper fileProcessingRecordMapper,
            DocumentService documentService,
            DocumentUploadProducer documentUploadProducer,
            MilvusVectorRepository milvusVectorRepository,
            FileValidationService fileValidationService,
            FileStorageService fileStorageService
    ) {
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
        this.documentService = documentService;
        this.documentUploadProducer = documentUploadProducer;
        this.milvusVectorRepository = milvusVectorRepository;
        this.fileValidationService = fileValidationService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 文档上传接口
     * @param file 上传的文件
     * @param principal 当前用户信息
     * @return 返回上传结果，包含文件ID等信息
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        String userId = principal != null
                ? principal.getName()
                : "anonymous";

        // 1. 文件校验
        FileValidationService.ValidateFile validated =
                fileValidationService.validate(file);

        // 2. 生成业务文件ID
        String fileId = UUID.randomUUID().toString();

        // 3. 保存文件
        FileStorageService.StoredFile storedFile =
                fileStorageService.store(
                        file,
                        fileId,
                        validated.extension()
                );

        try {
            // 4. 保存文件处理记录
            FileProcessingRecord record = new FileProcessingRecord();
            record.setId(UUID.randomUUID());
            record.setFileId(fileId);
            record.setFilename(validated.originalFilename());
            record.setFileSize(validated.size());
            record.setStatus("PENDING");
            record.setFilePath(storedFile.filePath());
            record.setUploadTime(LocalDateTime.now());

            fileProcessingRecordMapper.insert(record);

            // 5. 发送 Kafka 消息
            DocumentUploadMessage message =
                    new DocumentUploadMessage();

            message.setFileId(fileId);
            message.setFileName(validated.originalFilename());
            message.setFileSize(String.valueOf(validated.size()));
            message.setFilePath(storedFile.filePath());
            message.setDocumentType(validated.extension());
            message.setUploadTime(LocalDateTime.now());

            documentUploadProducer.send(message);

        } catch (Exception exception) {
            // 数据库或消息发送失败时清理已保存文件
            fileStorageService.delete(storedFile.filePath());
            throw new BusinessException("文件上传处理失败", exception);
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "fileId", fileId,
                "status", "PENDING",
                "message", "文件已接收，正在处理中",
                "uploadedBy", userId
        ));
    }

    /**
     * 删除文档接口
     * @param fileId 文件ID
     * @return 返回删除结果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> delete(@PathVariable String fileId) {
        return ResponseEntity.ok(
                documentService.deleteDocument(fileId)
        );
    }

    /**
     * 查询文档列表接口
     * @param page 页码，默认为0
     * @param size 每页大小，默认为10，最大为100
     * @param status 文档状态，可选参数
     * @return 返回文档列表和分页信息
     */
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BusinessException("分页参数不合法");
        }

        int offset = page * size;

        List<FileProcessingRecord> records =
                fileProcessingRecordMapper.findAllPaged(
                        status,
                        size,
                        offset
                );

        int totalCount =
                fileProcessingRecordMapper.countAll(status);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "查询成功",
                "data", records,
                "totalCount", totalCount,
                "page", page,
                "size", size
        ));
    }

    /**
     * 重试处理失败的文档接口
     * @param fileId 文件ID
     * @return 返回重试结果
     */
    @PostMapping("/{fileId}/retry")
    public ResponseEntity<?> retry(@PathVariable String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException("文件ID不能为空");
        }

        FileProcessingRecord record =
                fileProcessingRecordMapper.findByFileId(fileId);

        if (record == null) {
            throw new BusinessException("文档不存在");
        }

        if (!"FAILED".equals(record.getStatus())) {
            throw new BusinessException("只有处理失败的文档才能重试");
        }

        // 删除旧向量
        milvusVectorRepository.deleteByFileId(fileId);

        // 重置数据库状态
        int updated =
                fileProcessingRecordMapper.resetToPending(fileId);

        if (updated == 0) {
            throw new BusinessException("重置文档状态失败");
        }

        // 重新发送 Kafka 消息
        DocumentUploadMessage message =
                new DocumentUploadMessage();

        message.setFileId(fileId);
        message.setFileName(record.getFilename());
        message.setFileSize(String.valueOf(record.getFileSize()));
        message.setFilePath(record.getFilePath());
        message.setDocumentType(
                fileValidationService.getExtension(
                        record.getFilename()
                )
        );
        message.setUploadTime(record.getUploadTime());
        message.setRetryCount(1);

        documentUploadProducer.send(message);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "文档已重新提交处理",
                "fileId", fileId,
                "status", "PENDING"
        ));
    }
}