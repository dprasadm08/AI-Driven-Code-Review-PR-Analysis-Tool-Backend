package com.aiprreview.controller;

import com.aiprreview.ai.AiProvider;
import com.aiprreview.ai.AiProviderRouter;
import com.aiprreview.ai.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final OpenAiService openAiService;
    private final AiProviderRouter aiProviderRouter;

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
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - analysis results");
        response.put("prId", prId);
        response.put("results", Map.of());
        return ResponseEntity.ok(response);
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
}
