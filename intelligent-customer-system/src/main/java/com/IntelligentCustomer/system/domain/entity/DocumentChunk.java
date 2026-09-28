package com.IntelligentCustomer.system.domain.entity;

import java.util.Map;

import lombok.Data;

/**
 * 文档类，用于存储文档相关信息
 * 该类包含文档ID、内容和元数据等属性
 */
@Data
public class DocumentChunk {
    /**
     * 文档唯一标识符
     * 用于唯一标识一个文档
     */
    private String id;
    /**
     * 文档内容
     * 存储文档的实际文本内容
     */
    private String content;
    /**
     * 文档元数据
     * 使用Map存储文档的附加信息，键为String类型，值为Object类型
     * 可以包含创建时间、作者、文档类型等信息
     */
    private Map<String, Object> metadata;
}
