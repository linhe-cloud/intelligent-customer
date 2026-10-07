package com.IntelligentCustomer.admin.web.controller;

import java.util.Map;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.IntelligentCustomer.system.repository.mapper.FileProcessingRecordMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/document")
public class DocumentStatusController {

    private final FileProcessingRecordMapper fileProcessingRecordMapper;

    public DocumentStatusController(FileProcessingRecordMapper fileProcessingRecordMapper) {
        this.fileProcessingRecordMapper = fileProcessingRecordMapper;
    }

    // 查询处理状态
    @GetMapping("/status/{fileId}")
    public ResponseEntity<?> getStatus(@PathVariable String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException("文件ID不能为空");
        }

        FileProcessingRecord record =
                fileProcessingRecordMapper.findByFileId(fileId);

        if (record == null) {
            throw new BusinessException(
                    "文件记录不存在: " + fileId,
                    404
            );
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("code", 200);
        response.put("fileId", fileId);
        response.put("status", record.getStatus());
        response.put("filename", record.getFilename());
        response.put("uploadTime", record.getUploadTime());

        // 如果处理完成，加上处理时间和统计数据
        if ("SUCCESS".equals(record.getStatus())) {
            response.put("processedChunks", record.getProcessedChunks());
            response.put("embeddingsCreated", record.getEmbeddingsCreated());
            response.put("processStartTime", record.getProcessStartTime());
            response.put("processEndTime", record.getProcessEndTime());
        } else if ("FAILED".equals(record.getStatus())) {
            response.put("failureReason", record.getFailureReason());
        }

        return ResponseEntity.ok(response);
    }
}
