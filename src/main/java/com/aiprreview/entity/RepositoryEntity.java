package com.aiprreview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repositories")
@CompoundIndex(name = "user_repo_idx", def = "{'userId': 1, 'fullName': 1}", unique = true)
public class RepositoryEntity {

    @Id
    private String id;

    @Indexed
    private String userId; // Reference to User who added this repository

    private String name; // Repository name (e.g., "my-repo")

    private String fullName; // Full name (e.g., "owner/my-repo")

    private String owner; // Repository owner username

    private String url; // GitHub repository URL

    private String description;

    private String defaultBranch;

    private String language; // Primary programming language

    @Builder.Default
    private Boolean isPrivate = false;

    @Builder.Default
    private Boolean isActive = true;

    private Long githubId; // GitHub repository ID

    private Integer stars;

    private Integer forks;

    private Integer openIssues;

    private String webhookId; // GitHub webhook ID if configured

    private Boolean webhookEnabled;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
