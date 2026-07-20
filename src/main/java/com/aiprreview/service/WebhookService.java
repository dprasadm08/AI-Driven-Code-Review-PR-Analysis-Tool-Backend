package com.aiprreview.service;

import com.aiprreview.analysis.UnifiedAnalysisService;
import com.aiprreview.config.WebhookAnalysisConfig;
import com.aiprreview.dto.webhook.GithubWebhookPayload;
import com.aiprreview.entity.PullRequest;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.entity.WebhookEvent;
import com.aiprreview.repository.PullRequestRepository;
import com.aiprreview.repository.RepositoryRepository;
import com.aiprreview.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;    private final UnifiedAnalysisService unifiedAnalysisService;
    private final WebhookAnalysisConfig webhookAnalysisConfig;    private final ObjectMapper objectMapper;

    @Value("${app.github.webhook.secret:}")
    private String webhookSecret;

    /**
     * Validate GitHub webhook signature
     */
    public boolean validateSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret is not configured. Signature validation will be skipped.");
            return true; // Allow webhook if secret is not configured (for development)
        }

        if (signature == null || !signature.startsWith("sha256=")) {
            log.error("Invalid signature format: {}", signature);
            return false;
        }

        try {
            String receivedSignature = signature.substring(7); // Remove "sha256=" prefix
            String calculatedSignature = calculateHmacSha256(payload, webhookSecret);
            
            boolean isValid = receivedSignature.equalsIgnoreCase(calculatedSignature);
            
            if (!isValid) {
                log.error("Signature validation failed. Expected: {}, Received: {}", 
                        calculatedSignature, receivedSignature);
            }
            
            return isValid;
        } catch (Exception ex) {
            log.error("Error validating webhook signature", ex);
            return false;
        }
    }

    /**
     * Calculate HMAC-SHA256 signature
     */
    private String calculateHmacSha256(String data, String key) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Handle pull request webhook event
     */
    public void handlePullRequestEvent(String eventType, GithubWebhookPayload payload, String rawPayload) {
        log.info("Processing pull_request event: action={}, repo={}, pr#{}",
                payload.getAction(),
                payload.getRepository().getFullName(),
                payload.getPullRequest().getNumber());

        // Save webhook event
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .eventType(eventType)
                .action(payload.getAction())
                .repositoryFullName(payload.getRepository().getFullName())
                .repositoryId(payload.getRepository().getId())
                .pullRequestNumber(payload.getPullRequest().getNumber())
                .pullRequestId(payload.getPullRequest().getId())
                .payload(rawPayload)
                .status("processing")
                .sender(payload.getSender().getLogin())
                .receivedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        webhookEvent = webhookEventRepository.save(webhookEvent);

        try {
            // Process based on action
            switch (payload.getAction()) {
                case "opened":
                    handlePullRequestOpened(payload, webhookEvent);
                    break;
                case "synchronize":
                    handlePullRequestSynchronize(payload, webhookEvent);
                    break;
                case "closed":
                    handlePullRequestClosed(payload, webhookEvent);
                    break;
                case "reopened":
                    handlePullRequestReopened(payload, webhookEvent);
                    break;
                default:
                    log.info("Unhandled pull_request action: {}", payload.getAction());
                    webhookEvent.setStatus("processed");
            }

            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

        } catch (Exception ex) {
            log.error("Error processing pull_request webhook", ex);
            webhookEvent.setStatus("failed");
            webhookEvent.setErrorMessage(ex.getMessage());
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
        }
    }

    /**
     * Handle pull request opened event
     */
    private void handlePullRequestOpened(GithubWebhookPayload payload, WebhookEvent webhookEvent) {
        log.info("Handling PR opened: #{} - {}", 
                payload.getPullRequest().getNumber(), 
                payload.getPullRequest().getTitle());

        // Find repository in our database
        Optional<RepositoryEntity> repositoryOpt = repositoryRepository
                .findByFullName(payload.getRepository().getFullName())
                .stream()
                .findFirst();

        if (repositoryOpt.isEmpty()) {
            log.warn("Repository not found in database: {}. Webhook event saved but not processed.",
                    payload.getRepository().getFullName());
            webhookEvent.setStatus("processed");
            webhookEvent.setErrorMessage("Repository not found in database");
            return;
        }

        RepositoryEntity repository = repositoryOpt.get();

        // Check if PR already exists
        Optional<PullRequest> existingPR = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repository.getId(), payload.getPullRequest().getNumber());

        if (existingPR.isPresent()) {
            log.info("Pull request already exists in database: #{}", payload.getPullRequest().getNumber());
            webhookEvent.setStatus("processed");
            return;
        }

        // Create new pull request
        PullRequest pullRequest = PullRequest.builder()
                .repositoryId(repository.getId())
                .userId(repository.getUserId())
                .githubId(payload.getPullRequest().getId())
                .prNumber(payload.getPullRequest().getNumber())
                .title(payload.getPullRequest().getTitle())
                .description(payload.getPullRequest().getDescription())
                .state(payload.getPullRequest().getState())
                .author(payload.getPullRequest().getUser().getLogin())
                .authorAvatarUrl(payload.getPullRequest().getUser().getAvatarUrl())
                .htmlUrl(payload.getPullRequest().getHtmlUrl())
                .headBranch(payload.getPullRequest().getHead().getRef())
                .headSha(payload.getPullRequest().getHead().getSha())
                .baseBranch(payload.getPullRequest().getBase().getRef())
                .baseSha(payload.getPullRequest().getBase().getSha())
                .isDraft(payload.getPullRequest().getDraft())
                .isMerged(payload.getPullRequest().getMerged())
                .analysisStatus("pending")
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pullRequest = pullRequestRepository.save(pullRequest);

        log.info("Successfully created pull request from webhook: #{} (ID: {})",
                pullRequest.getPrNumber(), pullRequest.getId());
        // Trigger automatic analysis asynchronously
        triggerAutomaticAnalysis(
                pullRequest.getId(),
                payload.getAction(),
                payload.getRepository().getFullName()
        );
        webhookEvent.setStatus("processed");
    }

    /**
     * Handle pull request synchronize event (new commits pushed)
     */
    private void handlePullRequestSynchronize(GithubWebhookPayload payload, WebhookEvent webhookEvent) {
        log.info("Handling PR synchronize: #{}", payload.getPullRequest().getNumber());

        Optional<RepositoryEntity> repositoryOpt = repositoryRepository
                .findByFullName(payload.getRepository().getFullName())
                .stream()
                .findFirst();

        if (repositoryOpt.isEmpty()) {
            webhookEvent.setStatus("processed");
            webhookEvent.setErrorMessage("Repository not found in database");
            return;
        }

        RepositoryEntity repository = repositoryOpt.get();

        Optional<PullRequest> prOpt = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repository.getId(), payload.getPullRequest().getNumber());

        if (prOpt.isEmpty()) {
            log.warn("Pull request not found: #{}", payload.getPullRequest().getNumber());
            webhookEvent.setStatus("processed");
            webhookEvent.setErrorMessage("Pull request not found in database");
            return;
        }

        // Update PR with new commit info
        PullRequest pullRequest = prOpt.get();
        pullRequest.setHeadSha(payload.getPullRequest().getHead().getSha());
        pullRequest.setUpdatedAt(LocalDateTime.now());
        pullRequest.setLastSyncedAt(LocalDateTime.now());
        // Reset analysis status since code changed
        pullRequest.setAnalysisStatus("pending");

        pullRequestRepository.save(pullRequest);

        log.info("Updated pull request from synchronize event: #{}", pullRequest.getPrNumber());
        // Trigger automatic analysis asynchronously
        triggerAutomaticAnalysis(
                pullRequest.getId(),
                payload.getAction(),
                payload.getRepository().getFullName()
        );
        webhookEvent.setStatus("processed");
    }

    /**
     * Handle pull request closed event
     */
    private void handlePullRequestClosed(GithubWebhookPayload payload, WebhookEvent webhookEvent) {
        log.info("Handling PR closed: #{}, merged={}", 
                payload.getPullRequest().getNumber(),
                payload.getPullRequest().getMerged());

        Optional<RepositoryEntity> repositoryOpt = repositoryRepository
                .findByFullName(payload.getRepository().getFullName())
                .stream()
                .findFirst();

        if (repositoryOpt.isEmpty()) {
            webhookEvent.setStatus("processed");
            return;
        }

        RepositoryEntity repository = repositoryOpt.get();

        Optional<PullRequest> prOpt = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repository.getId(), payload.getPullRequest().getNumber());

        if (prOpt.isEmpty()) {
            webhookEvent.setStatus("processed");
            return;
        }

        PullRequest pullRequest = prOpt.get();
        pullRequest.setState(payload.getPullRequest().getMerged() ? "merged" : "closed");
        pullRequest.setIsMerged(payload.getPullRequest().getMerged());
        pullRequest.setClosedAt(LocalDateTime.now());
        pullRequest.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(payload.getPullRequest().getMerged())) {
            pullRequest.setMergedAt(LocalDateTime.now());
        }

        pullRequestRepository.save(pullRequest);

        log.info("Updated pull request state to {}: #{}", pullRequest.getState(), pullRequest.getPrNumber());
        webhookEvent.setStatus("processed");
    }

    /**
     * Handle pull request reopened event
     */
    private void handlePullRequestReopened(GithubWebhookPayload payload, WebhookEvent webhookEvent) {
        log.info("Handling PR reopened: #{}", payload.getPullRequest().getNumber());

        Optional<RepositoryEntity> repositoryOpt = repositoryRepository
                .findByFullName(payload.getRepository().getFullName())
                .stream()
                .findFirst();

        if (repositoryOpt.isEmpty()) {
            webhookEvent.setStatus("processed");
            return;
        }

        RepositoryEntity repository = repositoryOpt.get();

        Optional<PullRequest> prOpt = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repository.getId(), payload.getPullRequest().getNumber());

        if (prOpt.isEmpty()) {
            webhookEvent.setStatus("processed");
            return;
        }

        PullRequest pullRequest = prOpt.get();
        pullRequest.setState("open");
        pullRequest.setUpdatedAt(LocalDateTime.now());

        pullRequestRepository.save(pullRequest);

        log.info("Reopened pull request: #{}", pullRequest.getPrNumber());
        // Trigger automatic analysis asynchronously
        triggerAutomaticAnalysis(
                pullRequest.getId(),
                payload.getAction(),
                payload.getRepository().getFullName()
        );
        webhookEvent.setStatus("processed");
    }

    /**
     * Trigger automatic AI analysis for a pull request (async, non-blocking)
     * Called after a PR event is saved and processed
     */
    @Async
    public void triggerAutomaticAnalysis(String pullRequestId, String action, String repositoryFullName) {
        if (!webhookAnalysisConfig.shouldTriggerAnalysis(action)) {
            log.debug("Webhook analysis disabled or action '{}' not in trigger list", action);
            return;
        }

        try {
            log.info("Triggering automatic analysis for PR {} (action={}) via webhook",
                    pullRequestId, action);

            unifiedAnalysisService.analyzeAndStore(
                    pullRequestId,
                    webhookAnalysisConfig.getProvider(),
                    null,  // no explicit token, will use user's stored token
                    webhookAnalysisConfig.isIncludeDiff()
            );

            log.info("Automatic analysis completed for PR {} ({}#{})",
                    pullRequestId, repositoryFullName, pullRequestId);

        } catch (Exception ex) {
            log.error("Automatic analysis failed for PR {} (action={}): {}",
                    pullRequestId, action, ex.getMessage());
            // Don't re-throw; async failures should be logged but not crash the webhook response
        }
    }
}

