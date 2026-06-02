package com.aiprreview.ai;

import com.aiprreview.config.OpenAiConfig;
import com.aiprreview.dto.openai.OpenAiRequest;
import com.aiprreview.dto.openai.OpenAiRequest.Message;
import com.aiprreview.dto.openai.OpenAiRequest.ResponseFormat;
import com.aiprreview.dto.openai.OpenAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_INSTRUCTION =
            "You are a senior software engineer performing pull request code reviews. " +
            "Always respond with valid JSON matching the schema described in the user message.";

    private final OpenAiConfig openAiConfig;

    // ----------------------------------------------------------------
    // Payload builder
    // ----------------------------------------------------------------

    public OpenAiRequest buildRequest(String userPrompt) {
        return OpenAiRequest.builder()
                .model(openAiConfig.getModel())
                .messages(List.of(
                        Message.builder().role(SYSTEM_ROLE).content(SYSTEM_INSTRUCTION).build(),
                        Message.builder().role(USER_ROLE).content(userPrompt).build()
                ))
                .maxTokens(openAiConfig.getMaxTokens())
                .temperature(openAiConfig.getTemperature())
                .responseFormat(ResponseFormat.builder().type("json_object").build())
                .build();
    }

    // ----------------------------------------------------------------
    // API call
    // ----------------------------------------------------------------

    public OpenAiResponse chat(String userPrompt) {
        validateApiKey();

        OpenAiRequest request = buildRequest(userPrompt);

        log.info("Sending request to OpenAI model={} maxTokens={}", request.getModel(), request.getMaxTokens());

        try {
            OpenAiResponse response = createWebClient()
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofSeconds(openAiConfig.getTimeoutSeconds()))
                    .block();

            if (response == null) {
                throw new OpenAiException("OpenAI returned an empty response");
            }

            log.info("OpenAI response received. finishReason={} totalTokens={}",
                    response.getChoices() != null && !response.getChoices().isEmpty()
                            ? response.getChoices().get(0).getFinishReason() : "unknown",
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : 0);

            return response;

        } catch (WebClientResponseException ex) {
            log.error("OpenAI API error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new OpenAiException("OpenAI API returned error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (OpenAiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error calling OpenAI", ex);
            throw new OpenAiException("Failed to call OpenAI API: " + ex.getMessage(), ex);
        }
    }

    // ----------------------------------------------------------------
    // Connectivity test — sends a minimal message to verify key + network
    // ----------------------------------------------------------------

    public ConnectivityResult testConnectivity() {
        if (openAiConfig.getApiKey() == null || openAiConfig.getApiKey().isBlank()) {
            return ConnectivityResult.failure("OPENAI_API_KEY is not configured.");
        }

        try {
            OpenAiRequest ping = OpenAiRequest.builder()
                    .model(openAiConfig.getModel())
                    .messages(List.of(
                            Message.builder().role(USER_ROLE).content("Reply with the single word: OK").build()
                    ))
                    .maxTokens(5)
                    .temperature(0)
                    .build();

            OpenAiResponse response = createWebClient()
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(ping)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return ConnectivityResult.success(openAiConfig.getModel(), response.firstContent());
            }
            return ConnectivityResult.failure("No choices returned from OpenAI");

        } catch (WebClientResponseException ex) {
            return ConnectivityResult.failure("HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ConnectivityResult.failure(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(openAiConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void validateApiKey() {
        if (openAiConfig.getApiKey() == null || openAiConfig.getApiKey().isBlank()) {
            throw new OpenAiException("OPENAI_API_KEY is not configured. Set the OPENAI_API_KEY environment variable.");
        }
    }

    // ----------------------------------------------------------------
    // Nested types
    // ----------------------------------------------------------------

    public static class OpenAiException extends RuntimeException {
        public OpenAiException(String message) { super(message); }
        public OpenAiException(String message, Throwable cause) { super(message, cause); }
    }

    public record ConnectivityResult(boolean success, String model, String message, String rawReply) {
        public static ConnectivityResult success(String model, String rawReply) {
            return new ConnectivityResult(true, model, "Connected successfully", rawReply);
        }
        public static ConnectivityResult failure(String message) {
            return new ConnectivityResult(false, null, message, null);
        }
    }
}

