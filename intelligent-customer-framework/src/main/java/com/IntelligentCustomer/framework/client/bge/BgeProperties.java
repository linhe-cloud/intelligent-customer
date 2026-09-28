package com.IntelligentCustomer.framework.client.bge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.bge")
public class BgeProperties {

    private String baseUrl;

    private String apiKey;

    private String model;

    private int dimension = 1024;
}
