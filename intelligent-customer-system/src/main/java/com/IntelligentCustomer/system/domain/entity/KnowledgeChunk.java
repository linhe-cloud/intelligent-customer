package com.IntelligentCustomer.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId("id")
    private UUID id;

    @TableField("file_id")
    private String fileId;

    private String content;

    @TableField("source_file")
    private String sourceFile;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
