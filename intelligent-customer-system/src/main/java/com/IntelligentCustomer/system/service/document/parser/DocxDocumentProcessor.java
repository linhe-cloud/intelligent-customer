package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * DocxDocumentProcessor 类实现了 DocumentProcessor 接口，用于处理 DOCX 格式的文档。
 * 该组件能够解析 DOCX 文件，提取其中的文本内容和表格，并将其转换为结构化的 ParsedDocument 对象。
 */
@Component
public class DocxDocumentProcessor implements DocumentProcessor {

    /**
     * 检查当前处理器是否支持指定的文件扩展名。
     *
     * @param extension 文件扩展名
     * @return 如果支持返回 true，否则返回 false
     */
    @Override
    public boolean supports(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }

    /**
     * 解析 DOCX 文档输入流，提取文档内容和元数据。
     *
     * @param input 包含文档输入流和文件名的 DocumentInput 对象
     * @return 解析后的 ParsedDocument 对象，包含文档标题、元数据和各个章节
     * @throws BusinessException 如果文档解析失败或文档中没有有效文本
     */
    @Override
    public ParsedDocument parse(DocumentInput input) {
        try (XWPFDocument document = new XWPFDocument(input.getInputStream())) {

            ParsedDocument result = new ParsedDocument();
            result.setTitle(input.getFileName());
            result.getMetadata().put("fileType", "docx");

            int order = 0;

            // 遍历文档中的所有元素（段落和表格）
            for (IBodyElement element : document.getBodyElements()) {

                // 处理段落元素
                if (element instanceof XWPFParagraph paragraph) {
                    String content = paragraph.getText();

                    // 跳过空段落
                    if (content == null || content.isBlank()) {
                        continue;
                    }

                    ParsedSection section = new ParsedSection();
                    section.setOrder(order++);
                    section.setHeading(resolveHeading(paragraph));
                    section.setContent(content);

                    result.getSections().add(section);
                }

                // 处理表格元素
                if (element instanceof XWPFTable table) {
                    String content = convertTable(table);

                    // 跳过空表格
                    if (content.isBlank()) {
                        continue;
                    }

                    ParsedSection section = new ParsedSection();
                    section.setOrder(order++);
                    section.setHeading("表格");
                    section.setContent(content);

                    result.getSections().add(section);
                }
            }

            // 如果没有找到有效内容，抛出异常
            if (result.getSections().isEmpty()) {
                throw new BusinessException("DOCX 文件没有有效文本", 422);
            }

            return result;

        } catch (IOException e) {
            throw new BusinessException("DOCX 文件解析失败", 422, e);
        }
    }

    /**
     * 解析段落样式，判断是否为标题。
     *
     * @param paragraph XWPFParagraph 对象
     * @return 如果是标题则返回标题文本，否则返回 null
     */
    private String resolveHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();

        if (style != null
                && style.toLowerCase().startsWith("heading")) {
            return paragraph.getText();
        }

        return null;
    }

    /**
     * 将表格内容转换为字符串格式。
     *
     * @param table XWPFTable 对象
     * @return 表格内容的字符串表示，单元格内容用竖线分隔
     */
    private String convertTable(XWPFTable table) {
        StringBuilder result = new StringBuilder();

        // 遍历表格的每一行
        for (XWPFTableRow row : table.getRows()) {
            // 遍历行中的每个单元格
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();

                // 将非空单元格内容添加到结果中
                if (text != null && !text.isBlank()) {
                    result.append(text.trim()).append(" | ");
                }
            }

            result.append("\n");
        }

        return result.toString();
    }
}
