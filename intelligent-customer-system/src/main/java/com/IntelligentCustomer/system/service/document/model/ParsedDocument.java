package com.IntelligentCustomer.system.service.document.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析后的文档类，用于存储文档的解析结果
 * 包含文档标题、章节列表和元数据信息
 */
@Data  // 使用Lombok的@Data注解，自动生成getter、setter、toString等方法
public class ParsedDocument {

    private String title;  // 文档标题

    /**
     * 文档章节列表
     * 使用List存储ParsedSection对象，初始化为ArrayList
     */
    private List<ParsedSection> sections = new ArrayList<>();

    /**
     * 文档元数据
     * 使用Map存储键值对形式的元数据，键为String类型，值为Object类型
     * 初始化为HashMap
     */
    private Map<String, Object> metadata = new HashMap<>();
}
