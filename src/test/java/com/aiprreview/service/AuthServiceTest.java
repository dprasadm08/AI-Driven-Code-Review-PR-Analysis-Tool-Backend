package com.aiprreview.service;

import com.aiprreview.dto.auth.AuthResponse;
import com.aiprreview.dto.auth.LoginRequest;
import com.aiprreview.dto.auth.SignupRequest;
import com.aiprreview.entity.User;
import com.aiprreview.exception.UnauthorizedException;
import com.aiprreview.repository.UserRepository;
import com.aiprreview.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signup_ShouldCreateUserAndReturnToken_WhenRequestIsValid() {
        SignupRequest request = SignupRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .fullName("Alice")
                .githubToken("ghp_x")
                .githubUsername("alice-gh")
                .build();

        User savedUser = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .fullName("Alice")
                .role("USER")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateTokenFromUsername("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.signup(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("alice", response.getUsername());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice", response.getFullName());
        assertEquals("USER", response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertEquals("alice", toSave.getUsername());
        assertEquals("alice@example.com", toSave.getEmail());
        assertEquals("encoded-pass", toSave.getPassword());
        assertEquals("USER", toSave.getRole());
        assertEquals(true, toSave.getEnabled());
        assertNotNull(toSave.getCreatedAt());
        assertNotNull(toSave.getUpdatedAt());
    }

    @Test
    void signup_ShouldThrow_WhenUsernameAlreadyExists() {
        SignupRequest request = SignupRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.signup(request));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_ShouldThrow_WhenEmailAlreadyExists() {
        SignupRequest request = SignupRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.signup(request));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldAuthenticateAndReturnResponse_WhenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("alice")
                .password("password123")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "password123");
        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .fullName("Alice")
                .role("USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByUsernameOrEmail("alice", "alice")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("alice", response.getUsername());
        assertEquals("alice@example.com", response.getEmail());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login_ShouldThrowUnauthorized_WhenAuthenticationFails() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("alice")
                .password("wrong")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("bad credentials"));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("Invalid username/email or password", ex.getMessage());
    }

    @Test
    void getCurrentUser_ShouldReturnUser_WhenUserExistsInSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "ignored")
        );

        User user = User.builder().username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        User result = authService.getCurrentUser();

        assertEquals("alice", result.getUsername());
    }

    @Test
    void getCurrentUser_ShouldThrowUnauthorized_WhenUserNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", "ignored")
        );

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, authService::getCurrentUser);

        assertEquals("User not found", ex.getMessage());
    }
}
