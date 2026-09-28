package com.IntelligentCustomer.system.service.document.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 统一文档输入源抽象，消除 MultipartFile 和 File 的双版本重复
 */
public interface DocumentInput {
    InputStream getInputStream() throws IOException;
    String getFileName();
    long getSize();
}
