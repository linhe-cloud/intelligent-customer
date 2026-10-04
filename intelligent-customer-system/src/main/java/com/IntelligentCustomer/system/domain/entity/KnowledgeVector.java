package com.IntelligentCustomer.system.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;


import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * 知识向量类，用于存储知识点的向量表示和相关元数据
 * 该类使用了Lombok的@Data注解，自动生成getter、setter、toString等方法
 */
@Data
public class KnowledgeVector {
    /**
     * 唯一标识符，用于标识每个知识点向量
     */
    private UUID id;
    /**
     * 文件ID，标识知识点所属的文件
     */
    private String fileId;
    /**
     * 知识点的内容文本
     */
    private String content;
    /**
     * 文本的向量表示，用于语义搜索和相似度计算
     */
    private float[] embedding;
    /**
     * 源文件路径或名称
     */
    private String sourceFile;
    /**
     * 块索引，标识知识点在源文件中的位置
     */
    private Integer chunkIndex;
    /**
     * 相关性得分，用于搜索结果排序
     */
    private Double score;
    /**
     * 知识点向量的创建时间
     */
    private LocalDateTime createdAt;
}
