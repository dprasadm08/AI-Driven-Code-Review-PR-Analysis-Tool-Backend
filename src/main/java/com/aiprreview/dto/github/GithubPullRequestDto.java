package com.aiprreview.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GithubPullRequestDto {

    private Long id;
    
    private Integer number;
    
    private String state; // open, closed
    
    private String title;
    
    @JsonProperty("body")
    private String description;
    
    private UserDto user;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("closed_at")
    private LocalDateTime closedAt;
    
    @JsonProperty("merged_at")
    private LocalDateTime mergedAt;
    
    @JsonProperty("merge_commit_sha")
    private String mergeCommitSha;
    
    private HeadDto head;
    
    private BaseDto base;
    
    private Boolean draft;
    
    private Boolean merged;
    
    private Boolean mergeable;
    
    @JsonProperty("mergeable_state")
    private String mergeableState;
    
    @JsonProperty("merged_by")
    private UserDto mergedBy;
    
    private Integer comments;
    
    @JsonProperty("review_comments")
    private Integer reviewComments;
    
    private Integer commits;
    
    private Integer additions;
    
    private Integer deletions;
    
    @JsonProperty("changed_files")
    private Integer changedFiles;
    
    private List<LabelDto> labels;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        private String type;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeadDto {
        private String ref; // branch name
        private String sha; // commit SHA
        private RepoDto repo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseDto {
        private String ref; // base branch name
        private String sha; // commit SHA
        private RepoDto repo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepoDto {
        private Long id;
        private String name;
        
        @JsonProperty("full_name")
        private String fullName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelDto {
        private Long id;
        private String name;
        private String color;
        private String description;
    }
}
