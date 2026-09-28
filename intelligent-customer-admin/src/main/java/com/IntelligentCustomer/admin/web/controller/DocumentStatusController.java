package com.IntelligentCustomer.admin.web.controller;

import java.util.Map;

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
        try {
            FileProcessingRecord record = fileProcessingRecordMapper.findByFileId(fileId);

            if (record == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "文件记录不存在: " + fileId
                ));
            }
            
            Map<String, Object> response = Map.of(
                "code", 200,
                "fileId", fileId,
                "status", record.getStatus(),
                "filename", record.getFilename(),
                "uploadTime", record.getUploadTime()
            );
            
            // 如果处理完成，加上处理时间
            if ("SUCCESS".equals(record.getStatus())) {
                response = Map.of(
                    "code", 200,
                    "fileId", fileId,
                    "status", record.getStatus(),
                    "filename", record.getFilename(),
                    "processedChunks", record.getProcessedChunks(),
                    "embeddingsCreated", record.getEmbeddingsCreated(),
                    "processStartTime", record.getProcessStartTime(),
                    "processEndTime", record.getProcessEndTime()
                );
            } else if ("FAILED".equals(record.getStatus())) {
                response = Map.of(
                    "code", 200,
                    "fileId", fileId,
                    "status", record.getStatus(),
                    "filename", record.getFilename(),
                    "failureReason", record.getFailureReason()
                );
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "文件记录不存在: " + fileId
            ));
        }
    }
}
