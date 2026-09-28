package com.IntelligentCustomer.system.domain.dto.kafka;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DocumentUploadMessage {

    private String fileId;                 // 文件唯一标识（UUID）
    private String fileName;               // 文件名
    private String fileSize;               // 文件大小
    private String filePath;               // 文件路径
    private String documentType;           // 文件类型
    private LocalDateTime uploadTime;      // 上传时间
    private int retryCount;                // 重试次数
}
