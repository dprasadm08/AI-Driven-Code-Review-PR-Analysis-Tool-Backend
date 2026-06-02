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
}
