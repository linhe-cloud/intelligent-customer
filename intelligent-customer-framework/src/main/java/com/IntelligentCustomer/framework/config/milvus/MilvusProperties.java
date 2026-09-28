package com.IntelligentCustomer.framework.config.milvus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {
    private String uri;
    private String token;
    private String database;
    private String collection;
    private int dimension = 1024;
    private String metricType = "COSINE";
    private boolean autoCreate = true;
}