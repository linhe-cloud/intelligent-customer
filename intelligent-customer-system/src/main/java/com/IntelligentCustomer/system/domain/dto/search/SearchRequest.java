package com.IntelligentCustomer.system.domain.dto.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 搜索请求参数。
 */
@Data
public class SearchRequest {

    @NotBlank(message = "搜索内容不能为空")
    private String query;

    @JsonProperty("topk")
    @Min(value = 1, message = "topk 必须大于 0")
    @Max(value = 20, message = "topk 不能超过 20")
    private Integer topK = 5;
}
