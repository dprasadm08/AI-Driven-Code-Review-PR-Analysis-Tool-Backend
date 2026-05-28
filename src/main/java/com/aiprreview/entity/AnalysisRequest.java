package com.aiprreview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_requests")
public class AnalysisRequest {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String repositoryId;

    @Indexed
    private String pullRequestId;

    private String headSha;

    private String baseSha;

    private List<String> files;

    private String status; // queued, in_progress, completed, failed

    private Integer priority;

    private String model;

    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String resultId;
}
