package com.IntelligentCustomer.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

/**
 * 文件处理记录实体类
 * 用于记录文件处理的各项信息，包括文件基本信息、处理状态、时间戳等
 */
@Data
@TableName("file_processing_record")
public class FileProcessingRecord {
    /**
     * 记录ID
     * 数据库表主键
     */
    @TableId("id")
    private UUID id;
    /**
     * 文件业务ID
     * 用于业务系统中的唯一标识
     */
    @TableField("file_id")
    private String fileId;  // 业务ID
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 文件大小
     * 单位：字节
     */
    @TableField("file_size")
    private long fileSize;
    /**
     * 文件处理状态
     * 表示文件当前的处理状态
     */
    private String status;  // 处理状态
    /**
     * 临时文件路径
     * 存储文件在系统中的临时存储路径
     */
    @TableField("file_path")
    private String filePath;  // 临时文件路径
    /**
     * 已处理的分块数量
     * 表示文件被切分为多少块进行处理
     */
    @TableField("processed_chunks")
    private Integer processedChunks;  // 切分数量
    /**
     * 创建的向量数量
     * 表示从文件中提取的向量数量
     */
    @TableField("embeddings_created")
    private Integer embeddingsCreated;  // 向量数量
    /**
     * 处理失败原因
     * 当文件处理失败时，记录具体的失败原因
     */
    @TableField("failure_reason")
    private String failureReason;  // 失败原因
    /**
     * 文件上传时间
     * 记录文件上传到系统的时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;  // 上传时间
    /**
     * 文件处理开始时间
     * 记录文件开始处理的时间点
     */
    @TableField("process_start_time")
    private LocalDateTime processStartTime;  // 处理开始时间
    /**
     * 文件处理结束时间
     * 记录文件处理完成的时间点
     */
    @TableField("process_end_time")
    private LocalDateTime processEndTime;  // 处理结束时间
}
