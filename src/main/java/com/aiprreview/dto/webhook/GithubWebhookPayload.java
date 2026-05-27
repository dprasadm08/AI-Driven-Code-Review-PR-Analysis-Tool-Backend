package com.aiprreview.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubWebhookPayload {

    private String action; // opened, closed, reopened, synchronize, edited
    
    private Integer number;
    
    @JsonProperty("pull_request")
    private PullRequestData pullRequest;
    
    private RepositoryData repository;
    
    private SenderData sender;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PullRequestData {
        private Long id;
        private Integer number;
        private String state;
        private String title;
        
        @JsonProperty("body")
        private String description;
        
        private UserData user;
        
        @JsonProperty("html_url")
        private String htmlUrl;
        
        private HeadData head;
        private BaseData base;
        
        private Boolean draft;
        private Boolean merged;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryData {
        private Long id;
        private String name;
        
        @JsonProperty("full_name")
        private String fullName;
        
        private OwnerData owner;
        
        @JsonProperty("html_url")
        private String htmlUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {
        private Long id;
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerData {
        private Long id;
        private String login;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderData {
        private Long id;
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeadData {
        private String ref;
        private String sha;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseData {
        private String ref;
        private String sha;
    }
}
