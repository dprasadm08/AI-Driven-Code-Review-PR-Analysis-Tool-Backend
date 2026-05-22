package com.aiprreview.controller;

import com.aiprreview.dto.pullrequest.PullRequestDetailResponse;
import com.aiprreview.dto.pullrequest.PullRequestResponse;
import com.aiprreview.service.PullRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    private final PullRequestService pullRequestService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PullRequestResponse>> getAllPullRequests(
            @RequestParam(required = false) String state) {
        log.info("Fetching all pull requests for authenticated user, state: {}", state);
        List<PullRequestResponse> pullRequests = pullRequestService.getAllPullRequests(state);
        return ResponseEntity.ok(pullRequests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PullRequestDetailResponse> getPullRequestById(@PathVariable String id) {
        log.info("Fetching pull request with id: {}", id);
        PullRequestDetailResponse pullRequest = pullRequestService.getPullRequestById(id);
        return ResponseEntity.ok(pullRequest);
    }

    @GetMapping("/repository/{repoId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PullRequestResponse>> getPullRequestsByRepository(
            @PathVariable String repoId,
            @RequestParam(required = false) String state) {
        log.info("Fetching pull requests for repository: {}, state: {}", repoId, state);
        List<PullRequestResponse> pullRequests = pullRequestService.getRepositoryPullRequests(repoId, state);
        return ResponseEntity.ok(pullRequests);
    }

    @PostMapping("/repository/{repoId}/sync")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> syncRepositoryPullRequests(
            @PathVariable String repoId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String token) {
        try {
            log.info("Syncing pull requests for repository: {}", repoId);
            List<PullRequestResponse> pullRequests = pullRequestService.syncRepositoryPullRequests(
                    repoId, state, token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully synced pull requests from GitHub");
            response.put("count", pullRequests.size());
            response.put("pullRequests", pullRequests);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to sync pull requests: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/repository/{repoId}/fetch/{prNumber}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> fetchPullRequest(
            @PathVariable String repoId,
            @PathVariable Integer prNumber,
            @RequestParam(required = false) String token) {
        try {
            log.info("Fetching pull request #{} for repository: {}", prNumber, repoId);
            PullRequestDetailResponse pullRequest = pullRequestService.fetchPullRequest(
                    repoId, prNumber, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(pullRequest);
        } catch (Exception ex) {
            log.error("Failed to fetch pull request: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getPullRequestCount(
            @RequestParam(required = false) String state) {
        log.info("Getting pull request count, state: {}", state);
        long count = pullRequestService.getPullRequestCount(state);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("state", state != null ? state : "all");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repository/{repoId}/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getRepositoryPullRequestCount(
            @PathVariable String repoId,
            @RequestParam(required = false) String state) {
        log.info("Getting pull request count for repository: {}, state: {}", repoId, state);
        long count = pullRequestService.getRepositoryPullRequestCount(repoId, state);
        Map<String, Object> response = new HashMap<>();
        response.put("repositoryId", repoId);
        response.put("count", count);
        response.put("state", state != null ? state : "all");
        return ResponseEntity.ok(response);
    }
}
