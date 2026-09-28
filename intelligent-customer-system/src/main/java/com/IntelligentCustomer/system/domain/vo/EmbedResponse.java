package com.IntelligentCustomer.system.domain.vo;

import lombok.Data;

@Data
public class EmbedResponse {
    private String text;    // 原始文本
    private int dimensions; // 向量维度
    private String status;  // 状态 如“success”或“error”
}
