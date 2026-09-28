package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentProcessorFactory {

    private final List<DocumentProcessor> processors;

    public DocumentProcessorFactory(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    public DocumentProcessor getProcessor(String fileName) {
        String extension = getExtension(fileName);
        return processors.stream()
                .filter(p -> p.supports(extension))
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的文件类型: " + fileName));
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
