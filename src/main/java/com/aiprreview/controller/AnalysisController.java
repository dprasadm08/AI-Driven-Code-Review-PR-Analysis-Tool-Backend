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
import com.aiprreview.dto.analysis.UnifiedAnalysisResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.entity.AnalysisResult;
import com.aiprreview.service.PullRequestService;
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
