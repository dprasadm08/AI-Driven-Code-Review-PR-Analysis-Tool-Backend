package com.aiprreview.ai;

import com.aiprreview.config.ClaudeConfig;
import com.aiprreview.dto.claude.ClaudeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClaudeServiceTest {

    @Test
    void getProviderName_ShouldReturnClaude() {
        ClaudeService service = new ClaudeService(defaultConfig());

        assertEquals("claude", service.getProviderName());
    }

    @Test
    void buildRequest_ShouldBuildExpectedPayload() {
        ClaudeConfig config = defaultConfig();
        config.setModel("claude-3-5-sonnet-20241022");
        config.setMaxTokens(222);
        config.setTemperature(0.7);

        ClaudeService service = new ClaudeService(config);
        ClaudeRequest request = service.buildRequest("Analyze this code");

        assertEquals("claude-3-5-sonnet-20241022", request.getModel());
        assertEquals(222, request.getMaxTokens());
        assertEquals(0.7, request.getTemperature());
        assertEquals(1, request.getMessages().size());
        assertEquals("user", request.getMessages().get(0).getRole());
        assertEquals("Analyze this code", request.getMessages().get(0).getContent());
    }

    @Test
    void message_ShouldThrow_WhenApiKeyMissing() {
        ClaudeConfig config = defaultConfig();
        config.setApiKey("  ");
        ClaudeService service = new ClaudeService(config);

        ClaudeService.ClaudeException ex = assertThrows(ClaudeService.ClaudeException.class,
                () -> service.message("hi"));

        assertEquals("CLAUDE_API_KEY is not configured. Set the CLAUDE_API_KEY environment variable.", ex.getMessage());
    }

    @Test
    void analyze_ShouldThrow_WhenApiKeyMissing() {
        ClaudeConfig config = defaultConfig();
        config.setApiKey("");
        ClaudeService service = new ClaudeService(config);

        assertThrows(ClaudeService.ClaudeException.class, () -> service.analyze("Analyze this"));
    }

    @Test
    void testConnectivity_ShouldReturnFailure_WhenApiKeyMissing() {
        ClaudeConfig config = defaultConfig();
        config.setApiKey(null);
        ClaudeService service = new ClaudeService(config);

        AiProvider.ConnectivityResult result = service.testConnectivity();

        assertEquals(false, result.success());
        assertEquals("claude", result.provider());
        assertEquals(config.getModel(), result.model());
        assertEquals("CLAUDE_API_KEY is not configured.", result.message());
    }

    private ClaudeConfig defaultConfig() {
        ClaudeConfig config = new ClaudeConfig();
        config.setApiKey("test-key");
        config.setModel("claude-3-5-sonnet-20241022");
        config.setBaseUrl("http://localhost");
        config.setAnthropicVersion("2023-06-01");
        config.setTimeoutSeconds(1);
        config.setMaxTokens(1000);
        config.setTemperature(0.2);
        config.setConnectTimeoutMillis(1000);
        config.setReadTimeoutSeconds(1);
        config.setWriteTimeoutSeconds(1);
        config.setRetryAttempts(1);
        config.setRetryBackoffMillis(1);
        config.setRetryMaxBackoffMillis(1);
        return config;
    }
}
