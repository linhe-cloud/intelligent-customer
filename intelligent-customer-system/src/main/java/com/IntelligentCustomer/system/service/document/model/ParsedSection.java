package com.IntelligentCustomer.system.service.document.model;

import lombok.Data;

/**
 * 解析后的章节类，用于存储文档解析后的章节相关信息
 * 使用@Data注解自动生成getter、setter、equals、hashCode和toString方法
 */
@Data
public class ParsedSection {

    // 章节顺序，用于标识章节在文档中的排列顺序
    private int order;

    // 章节标题，用于标识章节的主题
    private String heading;

    // 章节内容，包含章节的具体文本信息
    private String content;

    // 页码，标识章节所在的页数，使用Integer以便可以表示空值
    private Integer pageNumber;

    // 工作表名称，用于标识章节所在的工作表（适用于Excel等文档）
    private String sheetName;
}