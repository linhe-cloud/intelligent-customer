package com.IntelligentCustomer.system.domain.dto;

import lombok.Data;

/**
 * 表示嵌入请求的类，用于封装需要生成嵌入向量的文本数据
 * 使用@Data注解来自动生成getter、setter、toString等方法
 */
@Data
public class EmbedRequest {
    private String text; // 需要生成嵌入向量的文本内容
    
}
