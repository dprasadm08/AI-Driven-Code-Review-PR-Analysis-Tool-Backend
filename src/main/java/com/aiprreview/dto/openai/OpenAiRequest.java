package com.aiprreview.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiRequest {

    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;   // system | user | assistant
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type;   // text | json_object
    }
}
