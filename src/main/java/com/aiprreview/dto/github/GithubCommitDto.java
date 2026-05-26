package com.aiprreview.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubCommitDto {

    private String sha;
    
    @JsonProperty("node_id")
    private String nodeId;
    
    private CommitInfoDto commit;
    
    private String url;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    private AuthorDto author;
    
    private AuthorDto committer;
    
    private ParentDto[] parents;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitInfoDto {
        private AuthorInfoDto author;
        private AuthorInfoDto committer;
        private String message;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfoDto {
        private String name;
        private String email;
        private LocalDateTime date;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentDto {
        private String sha;
        private String url;
    }
}
