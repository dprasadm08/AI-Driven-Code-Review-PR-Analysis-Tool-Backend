package com.aiprreview.service;

import com.aiprreview.dto.auth.AuthResponse;
import com.aiprreview.dto.auth.LoginRequest;
import com.aiprreview.dto.auth.SignupRequest;
import com.aiprreview.entity.User;
import com.aiprreview.exception.UnauthorizedException;
import com.aiprreview.repository.UserRepository;
import com.aiprreview.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(SignupRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .githubToken(request.getGithubToken())
                .githubUsername(request.getGithubUsername())
                .role("USER")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // Generate JWT token
        String token = tokenProvider.generateTokenFromUsername(savedUser.getUsername());

        return new AuthResponse(
                token,
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String token = tokenProvider.generateToken(authentication);

            // Get user details
            User user = userRepository.findByUsernameOrEmail(
                    request.getUsernameOrEmail(),
                    request.getUsernameOrEmail()
            ).orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

            log.info("User logged in successfully: {}", user.getUsername());

            return new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );
        } catch (Exception ex) {
            log.error("Authentication failed: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid username/email or password");
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
