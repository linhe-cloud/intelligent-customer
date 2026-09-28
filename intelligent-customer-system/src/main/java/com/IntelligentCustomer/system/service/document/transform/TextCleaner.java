package com.IntelligentCustomer.system.service.document.transform;

import org.springframework.stereotype.Component;

/**
 * 文本清理工具类
 * 用于处理和清理文本内容，包括统一换行符、移除控制字符、规范化空格等
 */
@Component
public class TextCleaner {

    /**
     * 清理输入文本
     * 处理包括：
     * 1. 将回车换行符统一为换行符
     * 2. 移除除换行符和制表符外的所有控制字符
     * 3. 规范化空格和制表符
     * 4. 合并多个换行符
     * 5. 去除首尾空白
     *
     * @param text 需要清理的文本
     * @return 清理后的文本，如果输入为null则返回空字符串
     */
    public String clean(String text) {
        if (text == null) {  // 检查输入是否为null
            return "";       // 如果为null，返回空字符串
        }

        return text
                .replace("\r\n", "\n")    // 将Windows风格的回车换行符替换为换行符
                .replace('\r', '\n')       // 将单独的回车符替换为换行符
                .replace('\u00A0', ' ')    // 将不间断空格替换为普通空格
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")  // 移除除换行符和制表符外的所有控制字符
                .replaceAll("[ \\t]+", " ") // 将连续的空格和制表符替换为单个空格
                .replaceAll("\\n{3,}", "\n\n")  // 将三个或更多连续的换行符替换为两个换行符
                .trim();                   // 去除文本首尾的空白字符
    }
}
