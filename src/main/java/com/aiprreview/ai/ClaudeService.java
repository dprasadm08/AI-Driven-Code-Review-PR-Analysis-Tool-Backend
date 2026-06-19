package com.aiprreview.ai;

import com.aiprreview.config.ClaudeConfig;
import com.aiprreview.dto.claude.ClaudeRequest;
import com.aiprreview.dto.claude.ClaudeResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService implements AiProvider {

	private static final String USER_ROLE = "user";
	private static final String SYSTEM_INSTRUCTION =
			"You are a senior software engineer performing pull request code reviews. " +
					"Always respond with valid JSON matching the schema described in the user message.";

	private final ClaudeConfig claudeConfig;

	@Override
	public String getProviderName() {
		return "claude";
	}

	public ClaudeRequest buildRequest(String prompt) {
		return ClaudeRequest.builder()
				.model(claudeConfig.getModel())
				.system(SYSTEM_INSTRUCTION)
				.messages(List.of(ClaudeRequest.Message.builder().role(USER_ROLE).content(prompt).build()))
				.maxTokens(claudeConfig.getMaxTokens())
				.temperature(claudeConfig.getTemperature())
				.build();
	}

	public ClaudeResponse message(String prompt) {
		validateApiKey();

		ClaudeRequest request = buildRequest(prompt);

		try {
			ClaudeResponse response = createWebClient()
					.post()
					.uri("/messages")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(ClaudeResponse.class)
					.timeout(Duration.ofSeconds(claudeConfig.getTimeoutSeconds()))
					.retryWhen(buildRetrySpec())
					.block();

			if (response == null) {
				throw new ClaudeException("Claude returned an empty response");
			}
			return response;
		} catch (WebClientResponseException ex) {
			log.error("Claude API error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
			throw new ClaudeException("Claude API returned error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
		} catch (TimeoutException ex) {
			throw new ClaudeException(
					"Claude request timed out after " + claudeConfig.getTimeoutSeconds() + " seconds", ex);
		} catch (WebClientRequestException ex) {
			throw new ClaudeException("Network error calling Claude API: " + ex.getMessage(), ex);
		} catch (ClaudeException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Unexpected error calling Claude", ex);
			throw new ClaudeException("Failed to call Claude API: " + ex.getMessage(), ex);
		}
	}

	@Override
	public AiProviderResponse analyze(String prompt) {
		ClaudeResponse response = message(prompt);
		Map<String, Object> usage = new HashMap<>();
		if (response.getUsage() != null) {
			usage.put("inputTokens", response.getUsage().getInputTokens());
			usage.put("outputTokens", response.getUsage().getOutputTokens());
			usage.put("totalTokens", response.getUsage().getInputTokens() + response.getUsage().getOutputTokens());
		}
		return new AiProviderResponse(getProviderName(), response.firstContent(), usage);
	}

	@Override
	public ConnectivityResult testConnectivity() {
		if (claudeConfig.getApiKey() == null || claudeConfig.getApiKey().isBlank()) {
			return ConnectivityResult.failure(getProviderName(), claudeConfig.getModel(), "CLAUDE_API_KEY is not configured.");
		}

		try {
			ClaudeRequest ping = ClaudeRequest.builder()
					.model(claudeConfig.getModel())
					.messages(List.of(
							ClaudeRequest.Message.builder().role(USER_ROLE).content("Reply with the single word: OK").build()
					))
					.maxTokens(5)
					.temperature(0)
					.build();

			ClaudeResponse response = createWebClient()
					.post()
					.uri("/messages")
					.bodyValue(ping)
					.retrieve()
					.bodyToMono(ClaudeResponse.class)
					.timeout(Duration.ofSeconds(15))
					.retryWhen(buildRetrySpec())
					.block();

			if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
				return ConnectivityResult.success(getProviderName(), claudeConfig.getModel(), response.firstContent());
			}
			return ConnectivityResult.failure(getProviderName(), claudeConfig.getModel(), "No content returned from Claude");
		} catch (WebClientResponseException ex) {
			return ConnectivityResult.failure(getProviderName(), claudeConfig.getModel(),
					"HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
		} catch (Exception ex) {
			return ConnectivityResult.failure(getProviderName(), claudeConfig.getModel(), ex.getMessage());
		}
	}

	private WebClient createWebClient() {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, claudeConfig.getConnectTimeoutMillis())
				.responseTimeout(Duration.ofSeconds(claudeConfig.getReadTimeoutSeconds()))
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(claudeConfig.getReadTimeoutSeconds(), TimeUnit.SECONDS))
						.addHandlerLast(new WriteTimeoutHandler(claudeConfig.getWriteTimeoutSeconds(), TimeUnit.SECONDS)));

		return WebClient.builder()
				.baseUrl(claudeConfig.getBaseUrl())
				.defaultHeader("x-api-key", claudeConfig.getApiKey())
				.defaultHeader("anthropic-version", claudeConfig.getAnthropicVersion())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	private Retry buildRetrySpec() {
		return Retry.backoff(claudeConfig.getRetryAttempts(), Duration.ofMillis(claudeConfig.getRetryBackoffMillis()))
				.maxBackoff(Duration.ofMillis(claudeConfig.getRetryMaxBackoffMillis()))
				.filter(this::isRetryable)
				.doBeforeRetry(signal -> log.warn(
						"Retrying Claude request attempt={} reason={}",
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
			HttpStatus status = ex.getStatusCode();
			return status.is5xxServerError()
					|| status.value() == 429
					|| status.value() == 408;
		}
		return false;
	}

	private void validateApiKey() {
		if (claudeConfig.getApiKey() == null || claudeConfig.getApiKey().isBlank()) {
			throw new ClaudeException("CLAUDE_API_KEY is not configured. Set the CLAUDE_API_KEY environment variable.");
		}
	}

	public static class ClaudeException extends RuntimeException {
		public ClaudeException(String message) {
			super(message);
		}

		public ClaudeException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
