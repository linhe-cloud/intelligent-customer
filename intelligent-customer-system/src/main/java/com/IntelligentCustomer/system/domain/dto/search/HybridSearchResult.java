package com.IntelligentCustomer.system.domain.dto.search;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 混合搜索结果类，用于存储混合搜索（语义搜索和关键词搜索结合）的结果信息
 * 包含文档ID、文件内容、来源文件、分块索引、创建时间以及各种评分和排名信息
 */
@Data
public class HybridSearchResult {

    private UUID id;                    // 结果的唯一标识符

    private String fileId;              // 文件ID，标识结果所属的文件

    private String content;             // 文件内容的片段

    private String sourceFile;          // 来源文件的名称或路径

    private Integer chunkIndex;         // 内容在源文件中的分块索引

    private LocalDateTime createTime;   // 结果创建时间

    private Double semanticScore;       // Milvus 原始语义分数

    private Double keywordScore;        // Mysql FULLTEXT 分数

    private Integer semanticRank;       // 在语义搜索结果中的排名

    private Integer keywordRank;        // 在关键词搜索结果中的排名

    private Double fusedScore;          // 融合得分，综合语义得分和关键词得分计算得出的最终得分
}
