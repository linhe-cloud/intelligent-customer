package com.IntelligentCustomer.system.service.document.parser;


import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * TxtDocumentProcessor 类实现了 DocumentProcessor 接口，专门用于处理文本文件(.txt)的解析工作。
 * 通过@Component注解标记为Spring组件，可以被Spring容器管理。
 */
@Component
public class TxtDocumentProcessor implements DocumentProcessor {

    /**
     * 判断当前处理器是否支持处理指定扩展名的文件
     * @param extension 文件扩展名，如"txt"、"pdf"等
     * @return 如果扩展名为"txt"(不区分大小写)，则返回true，表示支持处理；否则返回false
     */
    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension);
    }

    /**
     * 解析文本文件内容并转换为结构化的ParsedDocument对象
     * @param input 包含文件输入流和文件信息的DocumentInput对象
     * @return 解析后的ParsedDocument对象，包含文件内容和元数据
     * @throws BusinessException 当文件内容为空或读取文件失败时抛出
     */
    @Override
    public ParsedDocument parse(DocumentInput input) {
        try (InputStream stream = input.getInputStream()) {
            // 使用UTF-8编码读取文件所有字节并转换为字符串
            String text = new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // 检查文本内容是否为空或仅包含空白字符
            if (text.isBlank()) {
                throw new BusinessException("TXT文件没有有效内容", 422);
            }

            // 创建并设置文本段落信息
            ParsedSection section = new ParsedSection();
            section.setOrder(0);  // 设置段落顺序为0
            section.setContent(text);  // 设置段落内容为整个文本内容

            // 创建并设置文档信息
            ParsedDocument document = new ParsedDocument();
            document.setTitle(input.getFileName());  // 设置文档标题为文件名
            document.getSections().add(section);  // 将段落添加到文档中
            // 添加文件元数据
            document.getMetadata().put("fileName", input.getFileName());
            document.getMetadata().put("fileType", "txt");

            return document;
        } catch (IOException e) {
            // 处理文件读取异常，包装为业务异常抛出
            throw new BusinessException("TXT文件解析失败", 422, e);
        }
    }
}
