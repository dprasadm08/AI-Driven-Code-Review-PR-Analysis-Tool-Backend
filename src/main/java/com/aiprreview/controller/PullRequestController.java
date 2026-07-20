package com.aiprreview.controller;

import com.aiprreview.dto.common.ApiResponse;
import com.aiprreview.dto.pullrequest.PullRequestDetailResponse;
import com.aiprreview.dto.pullrequest.PullRequestResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.service.PullRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pull-requests")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pull Requests", description = "Pull request listing, details, and GitHub sync")
public class PullRequestController {

    private final PullRequestService pullRequestService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<PullRequestResponse>>> getAllPullRequests(
            @RequestParam(required = false)
            @Pattern(regexp = "^(open|closed|merged)?$", message = "state must be open, closed, or merged")
            String state) {
        log.info("Fetching all pull requests for authenticated user, state: {}", state);
        List<PullRequestResponse> pullRequests = pullRequestService.getAllPullRequests(state);
        return ResponseEntity.ok(ApiResponse.success("Pull requests fetched successfully", pullRequests));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
        public ResponseEntity<ApiResponse<PullRequestDetailResponse>> getPullRequestById(@PathVariable @NotBlank String id) {
        log.info("Fetching pull request with id: {}", id);
        PullRequestDetailResponse pullRequest = pullRequestService.getPullRequestById(id);
        return ResponseEntity.ok(ApiResponse.success("Pull request fetched successfully", pullRequest));
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PullRequestWithFilesResponse>> getPullRequestDetails(
            @PathVariable @NotBlank String id,
            @RequestParam(required = false) @Size(max = 255) String token,
            @RequestParam(required = false, defaultValue = "false") boolean includeDiff) {
        log.info("Fetching pull request details with files for id: {}, includeDiff: {}", id, includeDiff);
        PullRequestWithFilesResponse pullRequest = pullRequestService.getPullRequestWithFiles(id, token, includeDiff);
        return ResponseEntity.ok(ApiResponse.success("Pull request details fetched successfully", pullRequest));
    }

    @GetMapping("/repository/{repoId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<PullRequestResponse>>> getPullRequestsByRepository(
            @PathVariable @NotBlank String repoId,
            @RequestParam(required = false)
            @Pattern(regexp = "^(open|closed|merged)?$", message = "state must be open, closed, or merged")
            String state) {
        log.info("Fetching pull requests for repository: {}, state: {}", repoId, state);
        List<PullRequestResponse> pullRequests = pullRequestService.getRepositoryPullRequests(repoId, state);
        return ResponseEntity.ok(ApiResponse.success("Repository pull requests fetched successfully", pullRequests));
    }

    @PostMapping("/repository/{repoId}/sync")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncRepositoryPullRequests(
            @PathVariable @NotBlank String repoId,
            @RequestParam(required = false)
            @Pattern(regexp = "^(open|closed|merged)?$", message = "state must be open, closed, or merged")
            String state,
            @RequestParam(required = false) @Size(max = 255) String token) {
        log.info("Syncing pull requests for repository: {}", repoId);
        List<PullRequestResponse> pullRequests = pullRequestService.syncRepositoryPullRequests(
                repoId, state, token);

        return ResponseEntity.ok(ApiResponse.success(
                "Successfully synced pull requests from GitHub",
                Map.of("count", pullRequests.size(), "pullRequests", pullRequests)
        ));
    }

    @PostMapping("/repository/{repoId}/fetch/{prNumber}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PullRequestDetailResponse>> fetchPullRequest(
            @PathVariable @NotBlank String repoId,
            @PathVariable @Positive Integer prNumber,
            @RequestParam(required = false) @Size(max = 255) String token) {
        log.info("Fetching pull request #{} for repository: {}", prNumber, repoId);
        PullRequestDetailResponse pullRequest = pullRequestService.fetchPullRequest(
                repoId, prNumber, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pull request fetched successfully", pullRequest));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPullRequestCount(
            @RequestParam(required = false)
            @Pattern(regexp = "^(open|closed|merged)?$", message = "state must be open, closed, or merged")
            String state) {
        log.info("Getting pull request count, state: {}", state);
        long count = pullRequestService.getPullRequestCount(state);
        return ResponseEntity.ok(ApiResponse.success(
                "Pull request count fetched successfully",
                Map.of("count", count, "state", state != null ? state : "all")
        ));
    }

    @GetMapping("/repository/{repoId}/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRepositoryPullRequestCount(
            @PathVariable @NotBlank String repoId,
            @RequestParam(required = false)
            @Pattern(regexp = "^(open|closed|merged)?$", message = "state must be open, closed, or merged")
            String state) {
        log.info("Getting pull request count for repository: {}, state: {}", repoId, state);
        long count = pullRequestService.getRepositoryPullRequestCount(repoId, state);
        return ResponseEntity.ok(ApiResponse.success(
                "Repository pull request count fetched successfully",
                Map.of(
                        "repositoryId", repoId,
                        "count", count,
                        "state", state != null ? state : "all"
                )
        ));
    }
}
