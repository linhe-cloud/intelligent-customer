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

@Component
public class BgeEmbeddingClient {

    private final RestClient restClient;
    private final BgeProperties properties;

    public BgeEmbeddingClient(RestClient.Builder restClientBuilder, BgeProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<float[]> embed(List<String> texts) {
        validateInput(texts);

        BgeEmbeddingRequest request = new BgeEmbeddingRequest(properties.getModel(), texts);
        BgeEmbeddingResponse response;

        try {
            response = restClient.post()
                    .uri("/embeddings")
                    .body(request)
                    .retrieve()
                    .body(BgeEmbeddingResponse.class);
        } catch (RestClientResponseException exception) {
            throw new BusinessException(
                    "BGE API 请求失败，HTTP 状态码：" + exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new BusinessException("无法连接 BGE API", exception);
        }

        return convertResponse(response, texts.size());
    }

    private List<float[]> convertResponse(BgeEmbeddingResponse response, int expectedCount) {
        if (response == null || response.getData() == null) {
            throw new BusinessException("BGE API 返回结果为空");
        }

        if (response.getData().size() != expectedCount) {
            throw new BusinessException(
                    "BGE 返回向量数量不正确，期望：" + expectedCount
                            + "，实际：" + response.getData().size()
            );
        }

        response.getData().sort(Comparator.comparingInt(data ->
                data.getIndex() == null ? Integer.MAX_VALUE : data.getIndex()
        ));

        List<float[]> result = new ArrayList<>(expectedCount);
        for (BgeEmbeddingResponse.EmbeddingData data : response.getData()) {
            List<Float> embedding = data.getEmbedding();

            if (embedding == null) {
                throw new BusinessException("BGE 返回了空向量");
            }

            if (embedding.size() != properties.getDimension()) {
                throw new BusinessException(
                        "BGE 向量维度不正确，配置维度：" + properties.getDimension()
                                + "，实际维度：" + embedding.size()
                );
            }

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i);
            }
            result.add(vector);
        }

        return result;
    }

    private void validateInput(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new BusinessException("待向量化文本不能为空");
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                throw new BusinessException("待向量化文本中存在空内容");
            }
        }
    }
}
