package com.IntelligentCustomer.system.service.document.parser;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel文档处理器实现类
 * 用于处理Excel文件（.xls和.xlsx格式）的解析工作
 */
@Component
public class ExcelDocumentProcessor implements DocumentProcessor {

    /**
     * 判断是否支持处理指定扩展名的文档
     * @param extension 文件扩展名
     * @return 如果支持xls或xlsx格式返回true，否则返回false
     */
    @Override
    public boolean supports(String extension) {
        return "xls".equalsIgnoreCase(extension) || "xlsx".equalsIgnoreCase(extension);
    }

    /**
     * 解析Excel文档输入流
     * @param input 文档输入流信息
     * @return 解析后的文档对象
     * @throws BusinessException 如果文件解析失败或没有有效数据
     */
    @Override
    public ParsedDocument parse(DocumentInput input) {
        try (Workbook workbook = WorkbookFactory.create(input.getInputStream())) {

            // 创建公式求值器，用于处理Excel中的公式
            FormulaEvaluator evaluator =
                    workbook.getCreationHelper().createFormulaEvaluator();

            // 创建数据格式化器，用于格式化单元格值
            DataFormatter formatter = new DataFormatter();

            // 初始化解析结果对象
            ParsedDocument result = new ParsedDocument();
            result.setTitle(input.getFileName());
            result.getMetadata().put("fileType", "excel");
            result.getMetadata().put(
                    "sheetCount",
                    workbook.getNumberOfSheets()
            );

            // 遍历工作簿中的所有工作表进行解析
            for (Sheet sheet : workbook) {
                parseSheet(sheet, evaluator, formatter, result);
            }

            // 如果没有找到有效数据，抛出业务异常
            if (result.getSections().isEmpty()) {
                throw new BusinessException("Excel文件没有有效数据");
            }

            return result;
        } catch (Exception e) {
            // 如果是业务异常则直接抛出，否则包装为新的业务异常
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("Excel文件解析失败", e);
        }
    }

    /**
     * 解析单个工作表
     * @param sheet 工作表对象
     * @param evaluator 公式求值器
     * @param formatter 数据格式化器
     * @param result 解析结果对象
     */
    private void parseSheet(
            Sheet sheet,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            ParsedDocument result
    ) {
        // 查找第一个非空行作为表头
        Row headerRow = findFirstNonEmptyRow(sheet, formatter, evaluator);

        // 如果没有找到表头行，则跳过该工作表
        if (headerRow == null) {
            return;
        }

        // 读取表头数据
        List<String> headers =
                readRow(headerRow, formatter, evaluator);

        // 设置最大处理行数，避免处理过大的文件
        int lastRow = Math.min(sheet.getLastRowNum(), 10000);

        // 遍历数据行
        for (int rowIndex = headerRow.getRowNum() + 1;
             rowIndex <= lastRow;
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            // 如果行不存在，跳过该行
            if (row == null) {
                continue;
            }

            // 读取行数据
            List<String> values =
                    readRow(row, formatter, evaluator);

            // 如果所有值都为空，跳过该行
            if (values.stream().allMatch(String::isBlank)) {
                continue;
            }

            // 构建内容字符串
            StringBuilder content = new StringBuilder();

            // 处理每列数据
            for (int column = 0; column < values.size(); column++) {
                String header = column < headers.size()
                        && !headers.get(column).isBlank()
                        ? headers.get(column)
                        : "列" + (column + 1);

                String value = values.get(column);

                // 如果值不为空，添加到内容中
                if (!value.isBlank()) {
                    content.append(header)
                            .append("：")
                            .append(value)
                            .append("\n");
                }
            }

            // 如果内容不为空，创建新的解析段落并添加到结果中
            if (!content.isEmpty()) {
                ParsedSection section = new ParsedSection();
                section.setOrder(result.getSections().size());
                section.setSheetName(sheet.getSheetName());
                section.setHeading(
                        sheet.getSheetName() + " 第" + (rowIndex + 1) + "行"
                );
                section.setContent(content.toString());
                result.getSections().add(section);
            }
        }
    }

    /**
     * 查找第一个非空行
     * @param sheet 工作表对象
     * @param formatter 数据格式化器
     * @param evaluator 公式求值器
     * @return 第一个非空行，如果没有则返回null
     */
    private Row findFirstNonEmptyRow(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        // 遍历工作表中的所有行
        for (Row row : sheet) {
            List<String> values = readRow(row, formatter, evaluator);
            // 如果行中存在非空值，则返回该行
            if (values.stream().anyMatch(value -> !value.isBlank())) {
                return row;
            }
        }
        return null;
    }

    /**
     * 读取行数据
     * @param row 行对象
     * @param formatter 数据格式化器
     * @param evaluator 公式求值器
     * @return 包含行所有单元格值的列表
     */
    private List<String> readRow(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        List<String> values = new ArrayList<>();
        // 获取最后一列的索引
        int lastCell = Math.max(row.getLastCellNum(), 0);

        // 遍历所有列
        for (int column = 0; column < lastCell; column++) {
            // 获取单元格，如果不存在则返回null
            Cell cell = row.getCell(
                    column,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            );

            // 格式化单元格值
            String value = cell == null
                    ? ""
                    : formatter.formatCellValue(cell, evaluator).trim();

            // 添加到值列表中
            values.add(value);
        }

        return values;
    }
}
