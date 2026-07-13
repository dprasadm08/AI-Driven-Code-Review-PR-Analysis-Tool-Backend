package com.aiprreview.controller;

import com.aiprreview.dto.common.ApiResponse;
import com.aiprreview.dto.webhook.GithubWebhookPayload;
import com.aiprreview.exception.UnauthorizedException;
import com.aiprreview.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "GitHub webhook receiver for pull request events")
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    /**
     * GitHub webhook endpoint for pull request events
     */
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<Map<String, String>>> handleGithubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook: event={}, delivery={}", eventType, deliveryId);

        // Validate signature
        if (!webhookService.validateSignature(rawPayload, signature)) {
            log.error("Webhook signature validation failed");
            throw new UnauthorizedException("Invalid webhook signature");
        }

        // Only process pull_request events
        if (!"pull_request".equals(eventType)) {
            log.info("Ignoring non-pull_request event: {}", eventType);
            return ResponseEntity.ok(ApiResponse.success(
                    "Event type not supported",
                    Map.of("event", eventType == null ? "" : eventType)
            ));
        }

        // Parse payload
        GithubWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, GithubWebhookPayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid webhook payload: " + ex.getMessage());
        }

        // Process event asynchronously
        webhookService.handlePullRequestEvent(eventType, payload, rawPayload);

        return ResponseEntity.ok(ApiResponse.success(
                "Webhook received successfully",
                Map.of(
                        "event", eventType,
                        "action", payload.getAction() == null ? "" : payload.getAction(),
                        "repository", payload.getRepository() != null && payload.getRepository().getFullName() != null
                                ? payload.getRepository().getFullName() : "",
                        "prNumber", payload.getPullRequest() != null && payload.getPullRequest().getNumber() != null
                                ? String.valueOf(payload.getPullRequest().getNumber()) : ""
                )
        ));
    }

    /**
     * Test endpoint to verify webhook is accessible
     */
    @GetMapping("/github")
    public ResponseEntity<ApiResponse<Map<String, String>>> testWebhook() {
        return ResponseEntity.ok(ApiResponse.success(
                "GitHub webhook endpoint is active",
                Map.of("status", "ready")
        ));
    }

    /**
     * Health check for webhook endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(
                "Webhook service is healthy",
                Map.of("status", "healthy", "service", "webhook")
        ));
    }
}
