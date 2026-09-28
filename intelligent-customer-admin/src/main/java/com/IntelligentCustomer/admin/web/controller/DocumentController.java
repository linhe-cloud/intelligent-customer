package com.IntelligentCustomer.admin.web.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.domain.dto.kafka.DocumentUploadMessage;
import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.kafka.producer.DocumentUploadProducer;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;
import com.IntelligentCustomer.system.service.document.DocumentService;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/document")
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".pdf", ".docx");

    private final FileProcessingRecordMapper fileProcessingRecordMapper;
    private final DocumentService documentService;
    private final DocumentUploadProducer documentUploadProducer;
    private final MilvusVectorRepository milvusVectorRepository;

    public DocumentController(FileProcessingRecordMapper fileProcessingRecordMapper,
                              DocumentService documentService,
                              DocumentUploadProducer documentUploadProducer,
                              MilvusVectorRepository milvusVectorRepository) {
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
        this.documentService = documentService;
        this.documentUploadProducer = documentUploadProducer;
        this.milvusVectorRepository = milvusVectorRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("code", 400, "message", "文件不能为空"));
        }

        // 文件类型校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> originalFilename.toLowerCase().endsWith(ext))) {
            throw new BusinessException("不支持的文件类型，仅允许 .txt, .pdf, .docx 格式");
        }

        // 生成 fileId
        String fileId = UUID.randomUUID().toString();

        // 保存临时文件
        String tempPath = "/tmp/uploads/" + fileId + "_" + file.getOriginalFilename();
        try {
            File destFile = new File(tempPath);
            destFile.getParentFile().mkdirs();
            file.transferTo(destFile);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "message", "文件保存失败"));
        }

        // 写入数据库记录（PENDING状态）
        FileProcessingRecord record = new FileProcessingRecord();
        record.setId(UUID.randomUUID());
        record.setFileId(fileId);
        record.setFilename(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setStatus("PENDING");
        record.setFilePath(tempPath);
        record.setUploadTime(LocalDateTime.now());
        fileProcessingRecordMapper.insert(record);

        // 发送 Kafka 消息
        DocumentUploadMessage message = new DocumentUploadMessage();
        message.setFileId(fileId);
        message.setFileName(file.getOriginalFilename());
        message.setFileSize(String.valueOf(file.getSize()));
        message.setFilePath(tempPath);
        message.setDocumentType(getExtension(file.getOriginalFilename()));
        message.setUploadTime(LocalDateTime.now());
        documentUploadProducer.send(message);

        // 返回 fileId
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "fileId", fileId,
            "status", "PENDING",
            "message", "文件已接收，正在处理中",
            "uploadedBy", userId
        ));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> delete(@PathVariable String fileId) {
        Map<String, Object> result = documentService.deleteDocument(fileId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        int offset = page * size;
        List<FileProcessingRecord> records = fileProcessingRecordMapper.findAllPaged(status, size, offset);
        int totalCount = fileProcessingRecordMapper.countAll(status);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "查询成功",
            "data", records,
            "totalCount", totalCount,
            "page", page,
            "size", size
        ));
    }

    @PostMapping("/{fileId}/retry")
    public ResponseEntity<?> retry(@PathVariable String fileId) {
        FileProcessingRecord record;
        try {
            record = fileProcessingRecordMapper.findByFileId(fileId);
        } catch (Exception e) {
            throw new BusinessException("文档不存在");
        }

        if (record == null) {
            throw new BusinessException("文档不存在");
        }

        if (!"FAILED".equals(record.getStatus())) {
            throw new BusinessException("只有处理失败的文档才能重试");
        }

        // 清理已有向量
        milvusVectorRepository.deleteBySourceFile(record.getFilename());

        // 重置状态为 PENDING
        int updated = fileProcessingRecordMapper.resetToPending(fileId);
        if (updated == 0) {
            throw new BusinessException("重置文档状态失败");
        }

        // 重新发送 Kafka 消息
        DocumentUploadMessage message = new DocumentUploadMessage();
        message.setFileId(fileId);
        message.setFileName(record.getFilename());
        message.setFileSize(String.valueOf(record.getFileSize()));
        message.setFilePath(record.getFilePath());
        message.setDocumentType(getExtension(record.getFilename()));
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

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "unknown";
    }
}
