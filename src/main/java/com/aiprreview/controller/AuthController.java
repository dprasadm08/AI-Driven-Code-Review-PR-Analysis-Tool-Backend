package com.aiprreview.controller;

import com.aiprreview.dto.auth.AuthResponse;
import com.aiprreview.dto.auth.LoginRequest;
import com.aiprreview.dto.auth.SignupRequest;
import com.aiprreview.dto.common.ApiResponse;
import com.aiprreview.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signup, login, and current user endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for username: {}", request.getUsername());
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User signed up successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        var user = authService.getCurrentUser();
        Map<String, Object> response = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "githubUsername", user.getGithubUsername() == null ? "" : user.getGithubUsername()
        );
        return ResponseEntity.ok(ApiResponse.success("Current user fetched successfully", response));
    }
}
