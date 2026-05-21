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
public class GithubRepositoryDto {

    private Long id;
    
    private String name;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private OwnerDto owner;
    
    @JsonProperty("private")
    private Boolean isPrivate;
    
    private String description;
    
    private String url;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("clone_url")
    private String cloneUrl;
    
    @JsonProperty("git_url")
    private String gitUrl;
    
    @JsonProperty("ssh_url")
    private String sshUrl;
    
    @JsonProperty("default_branch")
    private String defaultBranch;
    
    private String language;
    
    @JsonProperty("stargazers_count")
    private Integer stars;
    
    @JsonProperty("forks_count")
    private Integer forks;
    
    @JsonProperty("open_issues_count")
    private Integer openIssues;
    
    @JsonProperty("watchers_count")
    private Integer watchers;
    
    private Integer size;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("pushed_at")
    private LocalDateTime pushedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerDto {
        private Long id;
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        private String type;
    }
}
