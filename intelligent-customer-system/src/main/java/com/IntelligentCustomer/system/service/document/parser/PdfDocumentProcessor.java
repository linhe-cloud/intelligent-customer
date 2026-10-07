package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.IntelligentCustomer.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF 文件处理策略实现
 * 该类实现了DocumentProcessor接口，专门用于处理PDF文档的解析
 */
@Component
public class PdfDocumentProcessor implements DocumentProcessor {

    /**
     * 判断是否支持处理指定扩展名的文件
     * @param extension 文件扩展名
     * @return 如果是pdf文件则返回true，否则返回false
     */
    @Override
    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    /**
     * 解析PDF文档内容
     * @param input 包含PDF文件输入流和文件名的文档输入对象
     * @return 解析后的文档对象，包含标题、元数据和内容分段
     * @throws BusinessException 当PDF文件已加密或无法提取有效文本时抛出
     */
    @Override
    public ParsedDocument parse(DocumentInput input) {
        try (PDDocument pdf = PDDocument.load(input.getInputStream())) {
            // 检查PDF是否加密
            if (pdf.isEncrypted()) {
                throw new BusinessException("PDF文件已加密，无法解析", 422);
            }

            // 初始化解析结果对象
            ParsedDocument result = new ParsedDocument();
            result.setTitle(input.getFileName());
            result.getMetadata().put("fileType", "pdf");
            result.getMetadata().put("pageCount", pdf.getNumberOfPages());

            // 创建PDF文本提取器
            PDFTextStripper stripper = new PDFTextStripper();
            int validCharacterCount = 0;

            // 逐页提取PDF文本内容
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String content = stripper.getText(pdf);

                // 跳过空页面
                if (content == null || content.isBlank()) {
                    continue;
                }

                // 创建并设置页面段落信息
                ParsedSection section = new ParsedSection();
                section.setOrder(result.getSections().size());
                section.setPageNumber(page);
                section.setHeading("第" + page + "页");
                section.setContent(content);

                // 累计有效字符数
                validCharacterCount += content.trim().length();
                result.getSections().add(section);
            }

            // 检查是否提取到足够的有效文本
            if (validCharacterCount < 20) {
                throw new BusinessException(
                        "PDF未提取到有效文本，可能是扫描版PDF",
                        422
                );
            }

            return result;
        } catch (IOException e) {
            throw new BusinessException("PDF文件解析失败", 422, e);
        }

    }
}
