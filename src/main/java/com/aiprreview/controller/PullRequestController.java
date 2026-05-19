package com.aiprreview.controller;

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
@RequestMapping("/pull-requests")
@RequiredArgsConstructor
public class PullRequestController {

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllPullRequests() {
        log.info("Fetching all pull requests for authenticated user");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - requires valid JWT token");
        response.put("pullRequests", List.of());
        response.put("count", 0);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPullRequestById(@PathVariable String id) {
        log.info("Fetching pull request with id: {}", id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - pull request details");
        response.put("id", id);
        response.put("status", "requires authentication");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repository/{repoId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPullRequestsByRepository(@PathVariable String repoId) {
        log.info("Fetching pull requests for repository: {}", repoId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - repository pull requests");
        response.put("repositoryId", repoId);
        response.put("pullRequests", List.of());
        return ResponseEntity.ok(response);
    }
}
