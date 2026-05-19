package com.aiprreview.controller;

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
}
