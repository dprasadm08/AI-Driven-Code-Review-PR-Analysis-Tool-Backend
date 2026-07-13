package com.aiprreview.controller;

import com.aiprreview.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Service health, readiness, and liveness probes")
public class HealthController {

    @GetMapping
    @Operation(summary = "Service health check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> response = Map.of(
                "status", "UP",
                "service", "AI PR Review Backend",
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success("Health check successful", response));
    }
    
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        return ResponseEntity.ok(ApiResponse.success("Readiness check successful", Map.of("status", "READY")));
    }
    
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<Map<String, String>>> live() {
        return ResponseEntity.ok(ApiResponse.success("Liveness check successful", Map.of("status", "ALIVE")));
    }
}
