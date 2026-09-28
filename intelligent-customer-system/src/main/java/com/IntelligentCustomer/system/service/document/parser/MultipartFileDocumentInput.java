package com.IntelligentCustomer.system.service.document.parser;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * MultipartFile 适配器，将 Spring MultipartFile 适配为 DocumentInput
 */
public class MultipartFileDocumentInput implements DocumentInput {

    private final MultipartFile multipartFile;

    public MultipartFileDocumentInput(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return multipartFile.getInputStream();
    }

    @Override
    public String getFileName() {
        return multipartFile.getOriginalFilename();
    }

    @Override
    public long getSize() {
        return multipartFile.getSize();
    }
}
