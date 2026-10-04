package com.IntelligentCustomer.system.domain.dto.search;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关键词搜索结果类，用于存储关键词搜索后的结果信息
 * 包含了搜索结果的ID、文件ID、内容、源文件名、分片索引、创建时间和关键词得分等字段
 */
@Data
public class KeywordSearchResult {

    /**
     * 搜索结果的唯一标识符。
     *
     * MySQL 中的 knowledge_chunk.id 使用 CHAR(36) 保存 UUID，
     * 因此查询结果先使用 String 接收，再由业务层转换为 UUID。
     */
    private String id;

    /**
     * 所属文件的唯一标识符，用于关联到具体的文件
     */
    private String fileId;

    /**
     * 搜索结果的内容片段，包含关键词的文本内容
     */
    private String content;

    /**
     * 源文件的名称或路径，标识内容来源
     */
    private String sourceFile;

    /**
     * 内容在源文件中的分片索引，用于标识内容在文件中的位置
     */
    private Integer chunkIndex;

    /**
     * 结果记录的创建时间，使用LocalDateTime类型记录精确到纳秒的时间戳
     */
    private LocalDateTime createdAt;

    /**
     * 关键词得分，用于表示匹配程度的相关性评分
     */
    private Double keywordScore;
}
