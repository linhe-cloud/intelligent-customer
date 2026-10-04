package com.IntelligentCustomer.system.service.document.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 统一文档输入源抽象，消除 MultipartFile 和 File 的双版本重复
 * 该接口提供了一种统一的方式来处理不同类型的文档输入源，
 * 使得底层逻辑不依赖于具体的文件实现类，提高了代码的复用性和可扩展性。
 */
public interface DocumentInput {
    /**
     * 获取文档输入流
     * @return 文档内容的输入流
     * @throws IOException 当读取输入流发生错误时抛出
     */
    InputStream getInputStream() throws IOException;
    /**
     * 获取文档文件名
     * @return 文档的文件名
     */
    String getFileName();
    /**
     * 获取文档大小
     * @return 文档的大小（字节数）
     */
    long getSize();
}
