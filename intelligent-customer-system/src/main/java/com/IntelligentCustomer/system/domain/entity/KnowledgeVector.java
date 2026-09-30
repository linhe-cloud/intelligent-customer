package com.IntelligentCustomer.system.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;


@Data
public class KnowledgeVector {
    private UUID id;
    private String fileId;
    private String content;
    private float[] embedding;
    private String sourceFile;
    private Integer chunkIndex;
    private Double score;
    private LocalDateTime createdAt;
}
