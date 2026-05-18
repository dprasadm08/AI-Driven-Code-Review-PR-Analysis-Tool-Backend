package com.aiprreview.controller;

import com.aiprreview.dto.auth.AuthResponse;
import com.aiprreview.dto.auth.LoginRequest;
import com.aiprreview.dto.auth.SignupRequest;
import com.aiprreview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            log.info("Signup request received for username: {}", request.getUsername());
            AuthResponse response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.error("Signup failed: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            log.error("Signup failed with unexpected error", ex);
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred during signup");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login request received for: {}", request.getUsernameOrEmail());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username/email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            var user = authService.getCurrentUser();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole());
            response.put("githubUsername", user.getGithubUsername());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Get current user failed: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to fetch user details");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
