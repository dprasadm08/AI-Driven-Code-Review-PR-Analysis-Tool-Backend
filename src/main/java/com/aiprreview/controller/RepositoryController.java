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
@RequestMapping("/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllRepositories() {
        log.info("Fetching all repositories for authenticated user");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a protected endpoint - requires authentication");
        response.put("repositories", List.of());
        response.put("count", 0);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRepositoryById(@PathVariable String id) {
        log.info("Fetching repository with id: {}", id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint - repository details");
        response.put("id", id);
        response.put("status", "authenticated");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addRepository(@RequestBody Map<String, String> request) {
        log.info("Adding new repository: {}", request.get("name"));
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Repository would be added (protected endpoint)");
        response.put("name", request.get("name"));
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
