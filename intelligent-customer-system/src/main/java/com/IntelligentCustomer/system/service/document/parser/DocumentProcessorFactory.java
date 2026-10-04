package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档处理器工厂类，用于根据文件扩展名创建相应的文档处理器
 * 使用Spring框架的@Component注解标记为Spring组件
 */
@Component
public class DocumentProcessorFactory {

    /**
     * 文档处理器列表，用于存储所有可用的文档处理器实现
     * 通过构造函数注入，实现依赖注入
     */
    private final List<DocumentProcessor> processors;

    /**
     * 构造函数，注入所有DocumentProcessor类型的Bean
     * @param processors Spring容器中所有的DocumentProcessor实现
     */
    public DocumentProcessorFactory(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    /**
     * 根据文件名获取相应的文档处理器
     * @param fileName 文件名，包含扩展名
     * @return 匹配的文档处理器
     * @throws BusinessException 当文件类型不被支持时抛出业务异常
     */
    public DocumentProcessor getProcessor(String fileName) {
        String extension = getExtension(fileName);
        return processors.stream()
                .filter(p -> p.supports(extension))  // 过滤出支持该文件扩展名的处理器
                .findFirst()                        // 获取第一个匹配的处理器
                .orElseThrow(() -> new BusinessException("不支持的文件类型: " + fileName));  // 如果没有匹配的处理器则抛出异常
    }

    /**
     * 从文件名中提取文件扩展名
     * @param fileName 文件名，包含扩展名
     * @return 文件扩展名（小写），如果没有扩展名则返回空字符串
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {  // 检查文件名是否为null或不含点号
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();  // 获取点号后的部分并转为小写
    }
}
