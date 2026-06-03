package com.aiprreview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.claude")
public class ClaudeConfig {

    private String apiKey;
    private String model = "claude-3-5-sonnet-20241022";
    private String baseUrl = "https://api.anthropic.com/v1";
    private int maxTokens = 1000;
    private double temperature = 0.2;
    private int timeoutSeconds = 60;
    private String anthropicVersion = "2023-06-01";
}
