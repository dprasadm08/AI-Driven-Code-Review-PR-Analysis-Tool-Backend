package com.aiprreview.dto.repository;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryRequest {

    @NotBlank(message = "Repository full name is required (e.g., 'owner/repo')")
    @Size(min = 3, max = 255, message = "Repository full name must be between 3 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$", 
             message = "Repository full name must be in format 'owner/repo'")
    private String fullName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(min = 1, max = 255, message = "Default branch must be between 1 and 255 characters")
    private String defaultBranch;

    private Boolean enableWebhook;

    @Size(max = 255, message = "GitHub token must not exceed 255 characters")
    private String githubToken;
}
