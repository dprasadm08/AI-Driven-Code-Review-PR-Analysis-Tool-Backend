package com.aiprreview.controller;

import com.aiprreview.dto.repository.RepositoryRequest;
import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.service.GithubService;
import com.aiprreview.service.RepositoryService;
import jakarta.validation.Valid;
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
@RequestMapping("/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final GithubService githubService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addRepository(@Valid @RequestBody RepositoryRequest request) {
        try {
            log.info("Adding repository: {}", request.getFullName());
            RepositoryResponse response = repositoryService.addRepository(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.error("Failed to add repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            log.error("Unexpected error adding repository", ex);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add repository");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RepositoryResponse>> getAllRepositories(
            @RequestParam(required = false) Boolean activeOnly) {
        log.info("Fetching all repositories, activeOnly: {}", activeOnly);
        List<RepositoryResponse> repositories = (activeOnly != null && activeOnly) 
                ? repositoryService.getActiveRepositories()
                : repositoryService.getAllRepositories();
        return ResponseEntity.ok(repositories);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRepositoryById(@PathVariable String id) {
        try {
            log.info("Fetching repository with id: {}", id);
            RepositoryResponse response = repositoryService.getRepositoryById(id);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to fetch repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/name/{fullName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRepositoryByFullName(@PathVariable String fullName) {
        try {
            log.info("Fetching repository: {}", fullName);
            RepositoryResponse response = repositoryService.getRepositoryByFullName(fullName);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to fetch repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateRepository(
            @PathVariable String id,
            @Valid @RequestBody RepositoryRequest request) {
        try {
            log.info("Updating repository with id: {}", id);
            RepositoryResponse response = repositoryService.updateRepository(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to update repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteRepository(@PathVariable String id) {
        try {
            log.info("Deleting repository with id: {}", id);
            repositoryService.deleteRepository(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Repository deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to delete repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> toggleRepositoryStatus(@PathVariable String id) {
        try {
            log.info("Toggling status for repository with id: {}", id);
            RepositoryResponse response = repositoryService.toggleRepositoryStatus(id);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to toggle repository status: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getRepositoryCount() {
        log.info("Fetching repository count");
        Long count = repositoryService.getRepositoryCount();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RepositoryResponse>> searchRepositories(
            @RequestParam String query) {
        log.info("Searching repositories with query: {}", query);
        List<RepositoryResponse> repositories = repositoryService.searchRepositories(query);
        return ResponseEntity.ok(repositories);
    }

    @PostMapping("/sync/github")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> syncGithubRepositories(
            @RequestParam(required = false) String token) {
        try {
            log.info("Syncing repositories from GitHub");
            List<RepositoryResponse> repositories = githubService.syncUserRepositories(token);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully synced repositories from GitHub");
            response.put("count", repositories.size());
            response.put("repositories", repositories);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to sync GitHub repositories: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/sync/github/repo")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> fetchGithubRepository(
            @RequestParam String fullName,
            @RequestParam(required = false) String token) {
        try {
            log.info("Fetching repository from GitHub: {}", fullName);
            RepositoryResponse repository = githubService.fetchAndSaveRepository(fullName, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(repository);
        } catch (Exception ex) {
            log.error("Failed to fetch GitHub repository: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
