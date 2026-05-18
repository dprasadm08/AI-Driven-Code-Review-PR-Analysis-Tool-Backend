package com.aiprreview.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String email;
    private String fullName;
    private String role;

    public AuthResponse(String accessToken, String username, String email, String fullName, String role) {
        this.accessToken = accessToken;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
