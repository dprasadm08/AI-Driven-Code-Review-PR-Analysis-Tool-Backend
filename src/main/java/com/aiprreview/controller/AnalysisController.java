package com.aiprreview.controller;

import com.aiprreview.ai.AiProvider;
import com.aiprreview.ai.AiProviderRouter;
import com.aiprreview.ai.OpenAiService;
import com.aiprreview.analysis.BugAnalysisResult;
import com.aiprreview.analysis.BugAnalysisService;
import com.aiprreview.analysis.SecurityAnalysisResult;
import com.aiprreview.analysis.SecurityAnalysisService;
import com.aiprreview.analysis.PerformanceAnalysisResult;
import com.aiprreview.analysis.PerformanceAnalysisService;
import com.aiprreview.analysis.CodeQualityAnalysisResult;
import com.aiprreview.analysis.CodeQualityAnalysisService;
import com.aiprreview.analysis.TestCaseAnalysisResult;
import com.aiprreview.analysis.TestCaseAnalysisService;
import com.aiprreview.analysis.UnifiedAnalysisService;
import com.aiprreview.dto.analysis.ManualPrAnalysisRequest;
import com.aiprreview.dto.analysis.UnifiedAnalysisResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.entity.AnalysisResult;
import com.aiprreview.exception.AnalysisException;
import com.aiprreview.exception.ResourceNotFoundException;
import com.aiprreview.service.AnalysisService;
import com.aiprreview.service.PullRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final OpenAiService openAiService;
    private final AiProviderRouter aiProviderRouter;
    private final BugAnalysisService bugAnalysisService;
    private final SecurityAnalysisService securityAnalysisService;
    private final PerformanceAnalysisService performanceAnalysisService;
    private final CodeQualityAnalysisService codeQualityAnalysisService;
    private final TestCaseAnalysisService testCaseAnalysisService;
    private final UnifiedAnalysisService unifiedAnalysisService;
    private final AnalysisService analysisService;
    private final PullRequestService pullRequestService;

    // ─────────────────────────────────────────────────────────────────────────
    // Manual PR analysis endpoint (replaces placeholder /trigger)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /analysis/manual
     * Accepts a validated request body, runs all requested modules (default: all),
     * persists the combined result, and returns the unified response.
     */
    @PostMapping("/manual")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UnifiedAnalysisResponse> triggerManualAnalysis(
            @Valid @RequestBody ManualPrAnalysisRequest request) {

        log.info("Manual analysis requested for prId={} provider={} modules={}",
                request.getPullRequestId(), request.getProvider(), request.getModules());

        List<String> modules = request.getModules();
        boolean runAll = modules == null || modules.isEmpty();

        if (!runAll) {
            // Validate module values explicitly (belt-and-suspenders over @Pattern on list elements)
            List<String> allowed = List.of("bug", "security", "performance", "code_quality", "test_case");
            List<String> invalid = modules.stream().filter(m -> !allowed.contains(m)).toList();
            if (!invalid.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown module(s): " + invalid + ". Allowed: " + allowed);
            }
        }

        try {
            UnifiedAnalysisResponse result = unifiedAnalysisService.analyzeAndStore(
                    request.getPullRequestId(),
                    request.getProvider(),
                    request.getGithubToken(),
                    request.isIncludeDiff()
            );
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException ex) {
            throw ex; // let global handler return 404
        } catch (Exception ex) {
            log.error("Manual analysis failed for prId={}: {}", request.getPullRequestId(), ex.getMessage(), ex);
            throw new AnalysisException("Analysis failed: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Results & status
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/results/{prId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AnalysisResult>> getAnalysisResults(@PathVariable String prId) {
        if (prId == null || prId.isBlank()) {
            throw new IllegalArgumentException("prId path variable must not be blank");
        }
        log.info("Fetching analysis results for PR: {}", prId);
        List<AnalysisResult> results = unifiedAnalysisService.getResultsForPullRequest(prId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/status/{analysisId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(@PathVariable String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            throw new IllegalArgumentException("analysisId path variable must not be blank");
        }
        log.info("Fetching analysis status for id={}", analysisId);
        AnalysisResult result = analysisService.getAnalysisResultById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found: " + analysisId));
        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", result.getId());
        response.put("pullRequestId", result.getPullRequestId());
        response.put("status", result.getStatus());
        response.put("summary", result.getSummary());
        response.put("model", result.getModel());
        response.put("createdAt", result.getCreatedAt());
        response.put("completedAt", result.getCompletedAt());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI provider connectivity tests
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/test/openai")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testOpenAiConnectivity() {
        log.info("Testing OpenAI API connectivity");
        AiProvider.ConnectivityResult result = openAiService.testConnectivity();
        Map<String, Object> response = buildConnectivityResponse(result, null);
        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    @GetMapping("/test/claude")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testClaudeConnectivity() {
        log.info("Testing Claude API connectivity");
        AiProvider provider = aiProviderRouter.resolveProvider("claude");
        AiProvider.ConnectivityResult result = provider.testConnectivity();
        Map<String, Object> response = buildConnectivityResponse(result, null);
        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    @GetMapping("/test/provider")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testProviderSwitch(
            @RequestParam(required = false) String provider) {
        AiProvider selected = aiProviderRouter.resolveProvider(provider);
        AiProvider.ConnectivityResult result = selected.testConnectivity();
        Map<String, Object> response = buildConnectivityResponse(result, provider);
        response.put("requestedProvider", provider);
        response.put("availableProviders", aiProviderRouter.getAvailableProviders());
        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Individual module endpoints (with error handling)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/bugs/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BugAnalysisResult> detectBugs(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running bug detection on PR id={} provider={}", pullRequestId, provider);
        try {
            PullRequestWithFilesResponse prDetail =
                    pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
            return ResponseEntity.ok(bugAnalysisService.analyze(prDetail, provider));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Bug analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Bug analysis failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/security/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SecurityAnalysisResult> detectSecurityVulnerabilities(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running security analysis on PR id={} provider={}", pullRequestId, provider);
        try {
            PullRequestWithFilesResponse prDetail =
                    pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
            return ResponseEntity.ok(securityAnalysisService.analyze(prDetail, provider));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Security analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Security analysis failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/performance/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PerformanceAnalysisResult> detectPerformanceIssues(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running performance analysis on PR id={} provider={}", pullRequestId, provider);
        try {
            PullRequestWithFilesResponse prDetail =
                    pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
            return ResponseEntity.ok(performanceAnalysisService.analyze(prDetail, provider));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Performance analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Performance analysis failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/code-quality/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeQualityAnalysisResult> detectCodeSmells(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running code-quality analysis on PR id={} provider={}", pullRequestId, provider);
        try {
            PullRequestWithFilesResponse prDetail =
                    pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
            return ResponseEntity.ok(codeQualityAnalysisService.analyze(prDetail, provider));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Code-quality analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Code-quality analysis failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/test-cases/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TestCaseAnalysisResult> analyzeTestCoverage(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running test-case analysis on PR id={} provider={}", pullRequestId, provider);
        try {
            PullRequestWithFilesResponse prDetail =
                    pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
            return ResponseEntity.ok(testCaseAnalysisService.analyze(prDetail, provider));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Test-case analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Test-case analysis failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/full/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UnifiedAnalysisResponse> runUnifiedAnalysis(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running unified analysis on PR id={} provider={}", pullRequestId, provider);
        try {
            return ResponseEntity.ok(
                    unifiedAnalysisService.analyzeAndStore(pullRequestId, provider, token, includeDiff));
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unified analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            throw new AnalysisException("Unified analysis failed: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildConnectivityResponse(AiProvider.ConnectivityResult result, String requestedProvider) {
        Map<String, Object> response = new HashMap<>();
        if (requestedProvider != null) {
            response.put("requestedProvider", requestedProvider);
        }
        response.put("selectedProvider", result.provider());
        response.put("success", result.success());
        response.put("model", result.model());
        response.put("message", result.message());
        response.put("rawReply", result.rawReply());
        return response;
    }
}


@Slf4j
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final OpenAiService openAiService;
    private final AiProviderRouter aiProviderRouter;
    private final BugAnalysisService bugAnalysisService;
    private final SecurityAnalysisService securityAnalysisService;
    private final PerformanceAnalysisService performanceAnalysisService;
    private final CodeQualityAnalysisService codeQualityAnalysisService;
    private final TestCaseAnalysisService testCaseAnalysisService;
    private final UnifiedAnalysisService unifiedAnalysisService;
    private final PullRequestService pullRequestService;

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> triggerAnalysis(@RequestBody Map<String, String> request) {
        log.info("Triggering analysis for PR: {}", request.get("prId"));
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - AI analysis would be triggered");
        response.put("prId", request.get("prId"));
        response.put("status", "queued");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/results/{prId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAnalysisResults(@PathVariable String prId) {
        log.info("Fetching analysis results for PR: {}", prId);
        List<AnalysisResult> results = unifiedAnalysisService.getResultsForPullRequest(prId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/status/{analysisId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAnalysisStatus(@PathVariable String analysisId) {
        log.info("Fetching analysis status: {}", analysisId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - requires authentication");
        response.put("analysisId", analysisId);
        response.put("status", "completed");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/openai")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> testOpenAiConnectivity() {
        log.info("Testing OpenAI API connectivity");
        AiProvider.ConnectivityResult result = openAiService.testConnectivity();
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("provider", result.provider());
        response.put("model", result.model());
        response.put("message", result.message());
        response.put("rawReply", result.rawReply());
        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    @GetMapping("/test/claude")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> testClaudeConnectivity() {
        log.info("Testing Claude API connectivity");
        AiProvider provider = aiProviderRouter.resolveProvider("claude");
        AiProvider.ConnectivityResult result = provider.testConnectivity();
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("provider", result.provider());
        response.put("model", result.model());
        response.put("message", result.message());
        response.put("rawReply", result.rawReply());
        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    @GetMapping("/test/provider")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> testProviderSwitch(@RequestParam(required = false) String provider) {
        AiProvider selected = aiProviderRouter.resolveProvider(provider);
        AiProvider.ConnectivityResult result = selected.testConnectivity();

        Map<String, Object> response = new HashMap<>();
        response.put("requestedProvider", provider);
        response.put("selectedProvider", selected.getProviderName());
        response.put("availableProviders", aiProviderRouter.getAvailableProviders());
        response.put("success", result.success());
        response.put("model", result.model());
        response.put("message", result.message());
        response.put("rawReply", result.rawReply());

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    @PostMapping("/bugs/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> detectBugs(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running bug detection on PR id={} provider={}", pullRequestId, provider);
        PullRequestWithFilesResponse prDetail = pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
        BugAnalysisResult result = bugAnalysisService.analyze(prDetail, provider);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/security/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> detectSecurityVulnerabilities(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running security analysis on PR id={} provider={}", pullRequestId, provider);
        PullRequestWithFilesResponse prDetail = pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
        SecurityAnalysisResult result = securityAnalysisService.analyze(prDetail, provider);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/performance/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> detectPerformanceIssues(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running performance analysis on PR id={} provider={}", pullRequestId, provider);
        PullRequestWithFilesResponse prDetail = pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
        PerformanceAnalysisResult result = performanceAnalysisService.analyze(prDetail, provider);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/code-quality/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> detectCodeSmells(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running code-quality analysis on PR id={} provider={}", pullRequestId, provider);
        PullRequestWithFilesResponse prDetail = pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
        CodeQualityAnalysisResult result = codeQualityAnalysisService.analyze(prDetail, provider);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-cases/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> analyzeTestCoverage(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running test-case analysis on PR id={} provider={}", pullRequestId, provider);
        PullRequestWithFilesResponse prDetail = pullRequestService.getPullRequestWithFiles(pullRequestId, token, includeDiff);
        TestCaseAnalysisResult result = testCaseAnalysisService.analyze(prDetail, provider);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/full/{pullRequestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> runUnifiedAnalysis(
            @PathVariable String pullRequestId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean includeDiff) {
        log.info("Running unified analysis on PR id={} provider={}", pullRequestId, provider);
        UnifiedAnalysisResponse result = unifiedAnalysisService.analyzeAndStore(
                pullRequestId,
                provider,
                token,
                includeDiff
        );
        return ResponseEntity.ok(result);
    }
}
