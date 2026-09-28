package com.IntelligentCustomer.system.service.document;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import com.IntelligentCustomer.system.service.document.parser.DocumentInput;
import com.IntelligentCustomer.system.service.document.parser.DocumentProcessor;
import com.IntelligentCustomer.system.service.document.parser.DocumentProcessorFactory;
import com.IntelligentCustomer.system.service.document.parser.FileDocumentInput;
import com.IntelligentCustomer.system.service.document.transform.TextCleaner;
import com.IntelligentCustomer.system.service.document.transform.TextSplitter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentProcessorFactory processorFactory;
    private final TextCleaner textCleaner;
    private final TextSplitter textSplitter;

    public DocumentService(
            DocumentProcessorFactory processorFactory,
            TextCleaner textCleaner,
            TextSplitter textSplitter
    ) {
        this.processorFactory = processorFactory;
        this.textCleaner = textCleaner;
        this.textSplitter = textSplitter;
    }

    public List<DocumentChunk> parseAndSplit(
            String fileId,
            File file,
            String originalFilename
    ) {
        DocumentInput input =
                new FileDocumentInput(file, originalFilename);

        DocumentProcessor processor =
                processorFactory.getProcessor(originalFilename);

        ParsedDocument parsedDocument = processor.parse(input);

        for (ParsedSection section : parsedDocument.getSections()) {
            section.setContent(
                    textCleaner.clean(section.getContent())
            );
        }

        parsedDocument.getSections().removeIf(section ->
                section.getContent() == null
                        || section.getContent().isBlank()
        );

        if (parsedDocument.getSections().isEmpty()) {
            throw new BusinessException("文档没有可入库的有效文本");
        }

        return textSplitter.split(fileId, parsedDocument);
    }
}
