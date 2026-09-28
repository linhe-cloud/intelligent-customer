package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.system.service.document.model.ParsedDocument;

/**
 * 文档解析策略接口。
 * 每种文件格式只负责解析自身内容，不负责文本清洗、切片和向量化。
 */
public interface DocumentProcessor {

    /**
     * 判断解析器是否支持指定扩展名。
     */
    boolean supports(String extension);

    /**
     * 将文件解析成统一的结构化结果。
     */
    ParsedDocument parse(DocumentInput input);
}
