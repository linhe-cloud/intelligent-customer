package com.IntelligentCustomer.framework.client.bge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BgeEmbeddingRequest {

    private String model;

    private List<String> input;

    @JsonProperty("encoding_format")
    private String encodingFormat = "float";

    public BgeEmbeddingRequest(String model, List<String> input) {
        this.model = model;
        this.input = input;
    }
}
