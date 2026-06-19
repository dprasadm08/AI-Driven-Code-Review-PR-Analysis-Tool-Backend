package com.aiprreview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.openai")
public class OpenAiConfig {

    private String apiKey;
    private String model = "gpt-4";
    private String baseUrl = "https://api.openai.com/v1";
    private int maxTokens = 1000;
    private double temperature = 0.2;
    private int timeoutSeconds = 60;

    // Connection/read/write timeouts for HTTP client
    private int connectTimeoutMillis = 10000;
    private int readTimeoutSeconds = 60;
    private int writeTimeoutSeconds = 60;

    // Retry behavior for transient failures
    private int retryAttempts = 2;
    private int retryBackoffMillis = 1000;
    private int retryMaxBackoffMillis = 8000;
}
