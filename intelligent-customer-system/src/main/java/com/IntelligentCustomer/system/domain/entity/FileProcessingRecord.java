package com.IntelligentCustomer.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
@TableName("file_processing_record")
public class FileProcessingRecord {
    @TableId("id")
    private UUID id;
    @TableField("file_id")
    private String fileId;  // 业务ID
    private String filename;
    @TableField("file_size")
    private long fileSize;
    private String status;  // 处理状态
    @TableField("file_path")
    private String filePath;  // 临时文件路径
    @TableField("processed_chunks")
    private Integer processedChunks;  // 切分数量
    @TableField("embeddings_created")
    private Integer embeddingsCreated;  // 向量数量
    @TableField("failure_reason")
    private String failureReason;  // 失败原因
    @TableField("upload_time")
    private LocalDateTime uploadTime;  // 上传时间
    @TableField("process_start_time")
    private LocalDateTime processStartTime;  // 处理开始时间
    @TableField("process_end_time")
    private LocalDateTime processEndTime;  // 处理结束时间
}
