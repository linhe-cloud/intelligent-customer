package com.IntelligentCustomer.system.service.document.transform;

import com.IntelligentCustomer.system.domain.entity.DocumentChunk;
import com.IntelligentCustomer.system.service.document.model.ParsedDocument;
import com.IntelligentCustomer.system.service.document.model.ParsedSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文本分割器组件，用于将文档分割成多个小块
 */
@Component
public class TextSplitter {



    // 定义常量：块大小、重叠部分大小和最大搜索范围
    private static final int CHUNK_SIZE = 1000;  // 每个块的最大字符数
    private static final int OVERLAP = 200;      // 块之间的重叠字符数
    private static final int MAX_SEARCH = 100;  // 查找分割点的最大搜索范围

    /**
     * 将解析后的文档分割成多个文档块
     * @param fileId 文件ID
     * @param parsedDocument 已解析的文档对象
     * @return 分割后的文档块列表
     */
    public List<DocumentChunk> split(
            String fileId,
            ParsedDocument parsedDocument
    ) {
        List<DocumentChunk> result = new ArrayList<>();  // 存储分割后的文档块
        int chunkIndex = 0;  // 当前块的索引

        // 遍历文档的每个部分
        for (ParsedSection section : parsedDocument.getSections()) {
            String content = section.getContent();

            // 如果内容为空或空白，跳过当前部分
            if (content == null || content.isBlank()) {
                continue;
            }

            int start = 0;  // 当前块的起始位置

            // 循环处理内容，直到全部处理完毕
            while (start < content.length()) {
                // 计算当前块的结束位置
                int end = Math.min(
                        start + CHUNK_SIZE,
                        content.length()
                );

                // 如果不是最后一个块，查找合适的分割点
                if (end < content.length()) {
                    end = findBreakPoint(
                            content,
                            end,
                            MAX_SEARCH
                    );
                }

                // 提取当前块的内容并去除首尾空白
                String chunkContent =
                        content.substring(start, end).trim();

                // 如果块内容不为空，创建文档块对象
                if (!chunkContent.isBlank()) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setId(UUID.randomUUID().toString());  // 生成唯一ID
                    chunk.setContent(chunkContent);  // 设置块内容

                    // 创建并设置元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("fileId", fileId);  // 文件ID
                    metadata.put("chunkIndex", chunkIndex++);  // 块索引
                    metadata.put("heading", section.getHeading());  // 标题
                    metadata.put("pageNumber", section.getPageNumber());  // 页码
                    metadata.put("sheetName", section.getSheetName());  // 工作表名称

                    chunk.setMetadata(metadata);
                    result.add(chunk);  // 将块添加到结果列表
                }

                // 如果已经处理完整个内容，退出循环
                if (end >= content.length()) {
                    break;
                }

                // 计算下一个块的起始位置，考虑重叠部分
                start = Math.max(0, end - OVERLAP);
            }
        }

        return result;  // 返回分割后的文档块列表
    }

    /**
     * 查找合适的分割点
     * @param text 要分割的文本
     * @param chunkSize 块大小
     * @param maxSearch 最大搜索范围
     * @return 合适的分割点位置
     */
    private int findBreakPoint(
            String text,
            int chunkSize,
            int maxSearch
    ) {
        // 计算最大搜索位置
        int maxPosition = Math.min(
                chunkSize + maxSearch,
                text.length()
        );

        // 从chunkSize开始搜索，直到找到合适的分割点
        for (int i = chunkSize; i < maxPosition; i++) {
            char current = text.charAt(i);

            // 检查当前字符是否是分割点（换行符、句号等）
            if (current == '\n'
                    || current == '。'
                    || current == '.'
                    || current == '！'
                    || current == '？'
                    || current == ' ') {
                return i + 1;  // 返回分割点后的位置
            }
        }

        return chunkSize;  // 如果没找到合适的分割点，返回原chunkSize
    }
}
