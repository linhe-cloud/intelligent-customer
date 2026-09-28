package com.IntelligentCustomer.framework.config.document;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 文档配置类
 * 用于配置文档处理相关的属性，包括上传目录、文件大小限制、分片参数等
 * 通过@ConfigurationProperties注解将配置文件中以"document"为前缀的属性绑定到此类
 */
@Data
@Component
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {
    // 文件上传目录，默认为"./data/uploads"
    private String uploadDirectory = "./data/uploads";

    // 单个文件最大大小，默认为20MB（20 * 1024 * 1024字节）
    private long maxFileSize = 20 * 1024 * 1024;

    // 文件分片大小，默认为1000字节
    private int chunkSize = 1000;

    // 分片重叠大小，默认为150字节
    private int chunkOverlap = 150;

    // 最大分片数量，默认为1000
    private int maxChunks = 1000;

    // 向量嵌入处理时的批次大小，默认为16
    private int embeddingBatchSize = 16;

    // 允许上传的文件扩展名集合，默认包含txt、pdf、docx、xls、xlsx
    private Set<String> allowedExtensions = Set.of("txt", "pdf", "docx", "xls", "xlsx");
}
