package com.aiprreview.dto.pullrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestWithFilesResponse {

    // Basic PR info
    private String id;
    private String repositoryId;
    private String repositoryName;
    private String repositoryFullName;
    private Integer prNumber;
    private String title;
    private String description;
    private String state;
    private String author;
    private String authorAvatarUrl;
    private String htmlUrl;
    
    // Branch info
    private String headBranch;
    private String headSha;
    private String baseBranch;
    private String baseSha;
    
    // Status
    private Boolean isDraft;
    private Boolean isMerged;
    private Boolean isMergeable;
    
    // Statistics
    private Integer commentsCount;
    private Integer reviewCommentsCount;
    private Integer commitsCount;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    
    // Labels
    private List<String> labels;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime mergedAt;
    private LocalDateTime closedAt;
    
    // Files and commits
    private List<PullRequestFileResponse> files;
    private List<PullRequestCommitResponse> commits;
    
    // Diff (optional - can be large)
    private String diff;
}
