package com.aiprreview.ai;

import com.aiprreview.config.OpenAiConfig;
import com.aiprreview.dto.openai.OpenAiRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiServiceTest {

    @Test
    void getProviderName_ShouldReturnOpenAi() {
        OpenAiService service = new OpenAiService(defaultConfig());

        assertEquals("openai", service.getProviderName());
    }

    @Test
    void buildRequest_ShouldBuildExpectedPayload() {
        OpenAiConfig config = defaultConfig();
        config.setModel("gpt-4");
        config.setMaxTokens(1234);
        config.setTemperature(0.4);

        OpenAiService service = new OpenAiService(config);
        OpenAiRequest request = service.buildRequest("Review this PR");

        assertEquals("gpt-4", request.getModel());
        assertEquals(1234, request.getMaxTokens());
        assertEquals(0.4, request.getTemperature());
        assertNotNull(request.getResponseFormat());
        assertEquals("json_object", request.getResponseFormat().getType());
        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).getRole());
        assertEquals("user", request.getMessages().get(1).getRole());
        assertEquals("Review this PR", request.getMessages().get(1).getContent());
    }

    @Test
    void chat_ShouldThrow_WhenApiKeyMissing() {
        OpenAiConfig config = defaultConfig();
        config.setApiKey("   ");
        OpenAiService service = new OpenAiService(config);

        OpenAiService.OpenAiException ex = assertThrows(OpenAiService.OpenAiException.class,
                () -> service.chat("hello"));

        assertEquals("OPENAI_API_KEY is not configured. Set the OPENAI_API_KEY environment variable.", ex.getMessage());
    }

    @Test
    void analyze_ShouldThrow_WhenApiKeyMissing() {
        OpenAiConfig config = defaultConfig();
        config.setApiKey("");
        OpenAiService service = new OpenAiService(config);

        assertThrows(OpenAiService.OpenAiException.class, () -> service.analyze("analyze"));
    }

    @Test
    void testConnectivity_ShouldReturnFailure_WhenApiKeyMissing() {
        OpenAiConfig config = defaultConfig();
        config.setApiKey(null);
        OpenAiService service = new OpenAiService(config);

        AiProvider.ConnectivityResult result = service.testConnectivity();

        assertEquals(false, result.success());
        assertEquals("openai", result.provider());
        assertEquals(config.getModel(), result.model());
        assertEquals("OPENAI_API_KEY is not configured.", result.message());
    }

    private OpenAiConfig defaultConfig() {
        OpenAiConfig config = new OpenAiConfig();
        config.setApiKey("test-key");
        config.setModel("gpt-4");
        config.setBaseUrl("http://localhost");
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
