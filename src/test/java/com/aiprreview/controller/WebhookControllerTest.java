package com.aiprreview.controller;

import com.aiprreview.exception.GlobalExceptionHandler;
import com.aiprreview.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "server.servlet.context-path=")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookService webhookService;

    @Test
    void testWebhookEndpoint_ShouldReturnReadyStatus() throws Exception {
        mockMvc.perform(get("/webhooks/github"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("GitHub webhook endpoint is active"))
                .andExpect(jsonPath("$.data.status").value("ready"));
    }

    @Test
    void healthWebhookEndpoint_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/webhooks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook service is healthy"))
                .andExpect(jsonPath("$.data.status").value("healthy"));
    }

    @Test
    void handleGithubWebhook_ShouldIgnoreUnsupportedEventType() throws Exception {
        String payload = "{\"action\":\"opened\"}";
        when(webhookService.validateSignature(payload, "sha256=valid")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Delivery", "delivery-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event type not supported"))
                .andExpect(jsonPath("$.data.event").value("push"));
    }

    @Test
    void handleGithubWebhook_ShouldReturnUnauthorized_WhenSignatureIsInvalid() throws Exception {
        String payload = "{\"action\":\"opened\"}";
        when(webhookService.validateSignature(payload, "sha256=invalid")).thenReturn(false);

        mockMvc.perform(post("/webhooks/github")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", "sha256=invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"));
    }

    @Test
    void handleGithubWebhook_ShouldProcessPullRequestEvent_WhenPayloadIsValid() throws Exception {
        String payload = """
                {
                  "action": "opened",
                  "repository": {
                    "id": 1001,
                    "name": "demo",
                    "full_name": "octocat/demo",
                    "owner": {"id": 10, "login": "octocat"},
                    "html_url": "https://github.com/octocat/demo"
                  },
                  "pull_request": {
                    "id": 501,
                    "number": 7,
                    "state": "open",
                    "title": "Add feature",
                    "body": "desc",
                    "user": {"id": 11, "login": "dev", "avatar_url": "http://avatar"},
                    "html_url": "https://github.com/octocat/demo/pull/7",
                    "head": {"ref": "feature", "sha": "abc123"},
                    "base": {"ref": "main", "sha": "def456"},
                    "draft": false,
                    "merged": false
                  },
                  "sender": {"id": 12, "login": "sender", "avatar_url": "http://avatar2"}
                }
                """;

        when(webhookService.validateSignature(payload, "sha256=valid")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Delivery", "delivery-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook received successfully"))
                .andExpect(jsonPath("$.data.event").value("pull_request"))
                .andExpect(jsonPath("$.data.action").value("opened"))
                .andExpect(jsonPath("$.data.repository").value("octocat/demo"))
                .andExpect(jsonPath("$.data.prNumber").value("7"));

        verify(webhookService).handlePullRequestEvent(eq("pull_request"), any(), eq(payload));
    }

    @Test
    void handleGithubWebhook_ShouldReturnBadRequest_WhenPayloadIsInvalidJson() throws Exception {
        String invalidPayload = "{\"action\":\"opened\",\"pull_request\":";
        when(webhookService.validateSignature(invalidPayload, "sha256=valid")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Invalid webhook payload:")));
    }
}
