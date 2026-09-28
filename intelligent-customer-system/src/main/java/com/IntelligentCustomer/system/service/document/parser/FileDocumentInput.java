package com.IntelligentCustomer.system.service.document.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File 适配器，将 java.io.File 适配为 DocumentInput
 * <p>
 * 构造时需显式传入 fileName，因为 File 对象的文件名可能不含原始名
 */
public class FileDocumentInput implements DocumentInput {

    private final File file;
    private final String fileName;

    public FileDocumentInput(File file, String fileName) {
        this.file = file;
        this.fileName = fileName;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getSize() {
        return file.length();
    }
}
