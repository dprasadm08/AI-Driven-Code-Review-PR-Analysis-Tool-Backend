package com.aiprreview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pull_requests")
@CompoundIndex(name = "repo_pr_idx", def = "{'repositoryId': 1, 'prNumber': 1}", unique = true)
@CompoundIndexes({
    @CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "repo_created_idx", def = "{'repositoryId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "user_state_idx", def = "{'userId': 1, 'state': 1}"),
    @CompoundIndex(name = "repo_state_idx", def = "{'repositoryId': 1, 'state': 1}")
})
public class PullRequest {

    @Id
    private String id;

    @Indexed
    private String repositoryId; // Reference to RepositoryEntity

    @Indexed
    private String userId; // Reference to User who owns the repository

    private Long githubId; // GitHub PR ID

    @Indexed
    private Integer prNumber; // Pull request number

    private String title;

    private String description;

    @Indexed
    private String state; // open, closed, merged

    private String author; // GitHub username of PR author

    private String authorAvatarUrl;

    private String htmlUrl; // GitHub PR URL

    private String headBranch; // Source branch

    private String headSha; // Source commit SHA

    private String baseBranch; // Target branch

    private String baseSha; // Base commit SHA

    private Boolean isDraft;

    private Boolean isMerged;

    private Boolean isMergeable;

    private String mergeableState;

    private String mergedBy; // Username who merged the PR

    private LocalDateTime mergedAt;

    private Integer commentsCount;

    private Integer reviewCommentsCount;

    private Integer commitsCount;

    private Integer additions;

    private Integer deletions;

    private Integer changedFiles;

    private List<String> labels;

    // Analysis fields
    private String analysisStatus; // pending, in_progress, completed, failed

    private String analysisResultId; // Reference to AnalysisResult

    private LocalDateTime analyzedAt;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;
}
