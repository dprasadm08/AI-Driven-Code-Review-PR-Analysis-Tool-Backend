package com.aiprreview.controller;

import com.aiprreview.dto.common.ApiResponse;
import com.aiprreview.dto.repository.RepositoryRequest;
import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.service.GithubService;
import com.aiprreview.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/repositories")
@RequiredArgsConstructor
@Tag(name = "Repositories", description = "Repository management and GitHub sync")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final GithubService githubService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> addRepository(@Valid @RequestBody RepositoryRequest request) {
        log.info("Adding repository: {}", request.getFullName());
        RepositoryResponse response = repositoryService.addRepository(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Repository added successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RepositoryResponse>>> getAllRepositories(
            @RequestParam(required = false) Boolean activeOnly) {
        log.info("Fetching all repositories, activeOnly: {}", activeOnly);
        List<RepositoryResponse> repositories = (activeOnly != null && activeOnly) 
                ? repositoryService.getActiveRepositories()
                : repositoryService.getAllRepositories();
        return ResponseEntity.ok(ApiResponse.success("Repositories fetched successfully", repositories));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> getRepositoryById(@PathVariable String id) {
        log.info("Fetching repository with id: {}", id);
        RepositoryResponse response = repositoryService.getRepositoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Repository fetched successfully", response));
    }

    @GetMapping("/name/{fullName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> getRepositoryByFullName(@PathVariable String fullName) {
        log.info("Fetching repository: {}", fullName);
        RepositoryResponse response = repositoryService.getRepositoryByFullName(fullName);
        return ResponseEntity.ok(ApiResponse.success("Repository fetched successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> updateRepository(
            @PathVariable String id,
            @Valid @RequestBody RepositoryRequest request) {
        log.info("Updating repository with id: {}", id);
        RepositoryResponse response = repositoryService.updateRepository(id, request);
        return ResponseEntity.ok(ApiResponse.success("Repository updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteRepository(@PathVariable String id) {
        log.info("Deleting repository with id: {}", id);
        repositoryService.deleteRepository(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Repository deleted successfully",
                Map.of("status", "deleted")
        ));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> toggleRepositoryStatus(@PathVariable String id) {
        log.info("Toggling status for repository with id: {}", id);
        RepositoryResponse response = repositoryService.toggleRepositoryStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Repository status updated successfully", response));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRepositoryCount() {
        log.info("Fetching repository count");
        Long count = repositoryService.getRepositoryCount();
        return ResponseEntity.ok(ApiResponse.success("Repository count fetched successfully", Map.of("count", count)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RepositoryResponse>>> searchRepositories(
            @RequestParam String query) {
        log.info("Searching repositories with query: {}", query);
        List<RepositoryResponse> repositories = repositoryService.searchRepositories(query);
        return ResponseEntity.ok(ApiResponse.success("Repository search completed", repositories));
    }

    @PostMapping("/sync/github")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncGithubRepositories(
            @RequestParam(required = false) String token) {
        log.info("Syncing repositories from GitHub");
        List<RepositoryResponse> repositories = githubService.syncUserRepositories(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Successfully synced repositories from GitHub",
                Map.of("count", repositories.size(), "repositories", repositories)
        ));
    }

    @PostMapping("/sync/github/repo")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RepositoryResponse>> fetchGithubRepository(
            @RequestParam String fullName,
            @RequestParam(required = false) String token) {
        log.info("Fetching repository from GitHub: {}", fullName);
        RepositoryResponse repository = githubService.fetchAndSaveRepository(fullName, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Repository fetched successfully", repository));
    }
}
