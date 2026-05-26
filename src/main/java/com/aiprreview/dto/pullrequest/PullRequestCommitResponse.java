package com.aiprreview.dto.pullrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestCommitResponse {

    private String sha;
    
    private String message;
    
    private String author;
    
    private String authorEmail;
    
    private String authorAvatarUrl;
    
    private LocalDateTime authoredAt;
    
    private String committer;
    
    private String committerEmail;
    
    private LocalDateTime committedAt;
    
    private String htmlUrl;
}
