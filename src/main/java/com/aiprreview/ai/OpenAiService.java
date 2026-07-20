package com.aiprreview.ai;

import com.aiprreview.config.OpenAiConfig;
import com.aiprreview.dto.openai.OpenAiRequest;
import com.aiprreview.dto.openai.OpenAiRequest.Message;
import com.aiprreview.dto.openai.OpenAiRequest.ResponseFormat;
import com.aiprreview.dto.openai.OpenAiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService implements AiProvider {

    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_INSTRUCTION =
            "You are a senior software engineer performing pull request code reviews. " +
            "Always respond with valid JSON matching the schema described in the user message.";

    private final OpenAiConfig openAiConfig;

    @Override
    public String getProviderName() {
        return "openai";
    }

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
                    .retryWhen(buildRetrySpec())
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
        } catch (WebClientRequestException ex) {
            throw new OpenAiException("Network error calling OpenAI API: " + ex.getMessage(), ex);
        } catch (OpenAiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (hasCause(ex, TimeoutException.class)) {
                throw new OpenAiException(
                        "OpenAI request timed out after " + openAiConfig.getTimeoutSeconds() + " seconds", ex);
            }
            log.error("Unexpected runtime error calling OpenAI", ex);
            throw new OpenAiException("Failed to call OpenAI API: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling OpenAI", ex);
            throw new OpenAiException("Failed to call OpenAI API: " + ex.getMessage(), ex);
        }
    }

    @Override
    public AiProviderResponse analyze(String prompt) {
        OpenAiResponse response = chat(prompt);
        String content = response.firstContent();
        java.util.Map<String, Object> usage = new java.util.HashMap<>();
        if (response.getUsage() != null) {
            usage.put("promptTokens", response.getUsage().getPromptTokens());
            usage.put("completionTokens", response.getUsage().getCompletionTokens());
            usage.put("totalTokens", response.getUsage().getTotalTokens());
        }
        return new AiProviderResponse(getProviderName(), content, usage);
    }

    // ----------------------------------------------------------------
    // Connectivity test — sends a minimal message to verify key + network
    // ----------------------------------------------------------------

    public ConnectivityResult testConnectivity() {
        if (openAiConfig.getApiKey() == null || openAiConfig.getApiKey().isBlank()) {
            return ConnectivityResult.failure(getProviderName(), openAiConfig.getModel(), "OPENAI_API_KEY is not configured.");
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
                    .retryWhen(buildRetrySpec())
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return ConnectivityResult.success(getProviderName(), openAiConfig.getModel(), response.firstContent());
            }
            return ConnectivityResult.failure(getProviderName(), openAiConfig.getModel(), "No choices returned from OpenAI");

        } catch (WebClientResponseException ex) {
            return ConnectivityResult.failure(getProviderName(), openAiConfig.getModel(),
                    "HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ConnectivityResult.failure(getProviderName(), openAiConfig.getModel(), ex.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private WebClient createWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, openAiConfig.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofSeconds(openAiConfig.getReadTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(openAiConfig.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(openAiConfig.getWriteTimeoutSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(openAiConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private Retry buildRetrySpec() {
        return Retry.backoff(openAiConfig.getRetryAttempts(), Duration.ofMillis(openAiConfig.getRetryBackoffMillis()))
                .maxBackoff(Duration.ofMillis(openAiConfig.getRetryMaxBackoffMillis()))
                .filter(this::isRetryable)
                .doBeforeRetry(signal -> log.warn(
                        "Retrying OpenAI request attempt={} reason={}",
                        signal.totalRetries() + 1,
                        signal.failure().getMessage()
                ));
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return status.is5xxServerError()
                    || status.value() == 429
                    || status.value() == 408;
        }
        return false;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

}

