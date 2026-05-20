package com.aiprreview.dto.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {

    private String id;
    private String name;
    private String fullName;
    private String owner;
    private String url;
    private String description;
    private String defaultBranch;
    private String language;
    private Boolean isPrivate;
    private Boolean isActive;
    private Long githubId;
    private Integer stars;
    private Integer forks;
    private Integer openIssues;
    private Boolean webhookEnabled;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
