package com.aiprreview.dto.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$", 
             message = "Repository full name must be in format 'owner/repo'")
    private String fullName; // e.g., "octocat/Hello-World"

    private String description;

    private String defaultBranch;

    private Boolean enableWebhook;

    private String githubToken; // Optional: user's GitHub token for private repos
}
