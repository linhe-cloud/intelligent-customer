package com.IntelligentCustomer.framework.client.bge;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.framework.client.bge.dto.BgeEmbeddingRequest;
import com.IntelligentCustomer.framework.client.bge.dto.BgeEmbeddingResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BGE嵌入客户端组件
 * 用于与BGE API交互，提供文本向量化的功能
 */
@Component
public class BgeEmbeddingClient {

    private final RestClient restClient; // REST客户端，用于发送HTTP请求
    private final BgeProperties properties; // BGE配置属性

    /**
     * 构造函数
     * @param restClientBuilder REST客户端构建器
     * @param properties BGE配置属性
     */
    public BgeEmbeddingClient(RestClient.Builder restClientBuilder, BgeProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl()) // 设置API基础URL
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey()) // 设置认证头
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 设置内容类型
                .build();
    }

    /**
     * 获取文本的向量表示
     * @param texts 待向量化的文本列表
     * @return 文本向量列表
     */
    public List<float[]> embed(List<String> texts) {
        validateInput(texts); // 验证输入文本

        BgeEmbeddingRequest request = new BgeEmbeddingRequest(properties.getModel(), texts); // 创建请求对象
        BgeEmbeddingResponse response;

        try {
            // 发送POST请求获取向量
            response = restClient.post()
                    .uri("/embeddings")
                    .body(request)
                    .retrieve()
                    .body(BgeEmbeddingResponse.class);
        } catch (RestClientResponseException exception) {
            // 处理HTTP响应异常
            throw new BusinessException(
                    "BGE API 请求失败，HTTP 状态码：" + exception.getStatusCode().value(),
                    502,
                    exception
            );
        } catch (RestClientException exception) {
            // 处理REST客户端异常
            throw new BusinessException("无法连接 BGE API", 502, exception);
        }

        return convertResponse(response, texts.size()); // 转换响应结果
    }

    /**
     * 转换API响应为向量列表
     * @param response API响应对象
     * @param expectedCount 期望的向量数量
     * @return 向量列表
     */
    private List<float[]> convertResponse(BgeEmbeddingResponse response, int expectedCount) {
        // 检查响应是否为空
        if (response == null || response.getData() == null) {
            throw new BusinessException("BGE API 返回结果为空", 502);
        }

        // 检查向量数量是否匹配
        if (response.getData().size() != expectedCount) {
            throw new BusinessException(
                    "BGE 返回向量数量不正确，期望：" + expectedCount
                            + "，实际：" + response.getData().size(),
                    502
            );
        }

        // 按索引排序向量数据
        response.getData().sort(Comparator.comparingInt(data ->
                data.getIndex() == null ? Integer.MAX_VALUE : data.getIndex()
        ));

        List<float[]> result = new ArrayList<>(expectedCount);
        // 处理每个向量数据
        for (BgeEmbeddingResponse.EmbeddingData data : response.getData()) {
            List<Float> embedding = data.getEmbedding();

            // 检查向量是否为空
            if (embedding == null) {
                throw new BusinessException("BGE 返回了空向量", 502);
            }

            // 检查向量维度是否匹配配置
            if (embedding.size() != properties.getDimension()) {
                throw new BusinessException(
                        "BGE 向量维度不正确，配置维度：" + properties.getDimension()
                                + "，实际维度：" + embedding.size(),
                        502
                );
            }

            // 将Float列表转换为基本float数组
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i);
            }
            result.add(vector);
        }

        return result;
    }

    /**
     * 验证输入文本
     * @param texts 待验证的文本列表
     */
    private void validateInput(List<String> texts) {
        // 检查文本列表是否为空
        if (texts == null || texts.isEmpty()) {
            throw new BusinessException("待向量化文本不能为空");
        }

        // 检查每个文本是否为空
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                throw new BusinessException("待向量化文本中存在空内容");
            }
        }
    }
}
