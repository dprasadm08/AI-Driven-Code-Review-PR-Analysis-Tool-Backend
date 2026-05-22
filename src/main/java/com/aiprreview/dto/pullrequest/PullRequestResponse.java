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
public class PullRequestResponse {

    private String id;
    
    private String repositoryId;
    
    private String repositoryName;
    
    private Long githubId;
    
    private Integer prNumber;
    
    private String title;
    
    private String description;
    
    private String state;
    
    private String author;
    
    private String authorAvatarUrl;
    
    private String htmlUrl;
    
    private String headBranch;
    
    private String baseBranch;
    
    private Boolean isDraft;
    
    private Boolean isMerged;
    
    private Boolean isMergeable;
    
    private Integer commentsCount;
    
    private Integer reviewCommentsCount;
    
    private Integer commitsCount;
    
    private Integer additions;
    
    private Integer deletions;
    
    private Integer changedFiles;
    
    private List<String> labels;
    
    private String analysisStatus;
    
    private String analysisResultId;
    
    private LocalDateTime analyzedAt;
    
    private LocalDateTime lastSyncedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime mergedAt;
    
    private LocalDateTime closedAt;
}
