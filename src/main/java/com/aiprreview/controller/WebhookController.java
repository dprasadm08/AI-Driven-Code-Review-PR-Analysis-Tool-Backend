package com.aiprreview.controller;

import com.aiprreview.dto.webhook.GithubWebhookPayload;
import com.aiprreview.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    /**
     * GitHub webhook endpoint for pull request events
     */
    @PostMapping("/github")
    public ResponseEntity<?> handleGithubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook: event={}, delivery={}", eventType, deliveryId);

        try {
            // Validate signature
            if (!webhookService.validateSignature(rawPayload, signature)) {
                log.error("Webhook signature validation failed");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Only process pull_request events
            if (!"pull_request".equals(eventType)) {
                log.info("Ignoring non-pull_request event: {}", eventType);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Event type not supported: " + eventType);
                return ResponseEntity.ok(response);
            }

            // Parse payload
            GithubWebhookPayload payload = objectMapper.readValue(rawPayload, GithubWebhookPayload.class);

            // Process event asynchronously (in real app, use @Async or message queue)
            webhookService.handlePullRequestEvent(eventType, payload, rawPayload);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Webhook received successfully");
            response.put("event", eventType);
            response.put("action", payload.getAction());
            response.put("repository", payload.getRepository().getFullName());
            response.put("prNumber", String.valueOf(payload.getPullRequest().getNumber()));

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error processing webhook", ex);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process webhook: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Test endpoint to verify webhook is accessible
     */
    @GetMapping("/github")
    public ResponseEntity<Map<String, String>> testWebhook() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "GitHub webhook endpoint is active");
        response.put("status", "ready");
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for webhook endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "webhook");
        return ResponseEntity.ok(response);
    }
}
