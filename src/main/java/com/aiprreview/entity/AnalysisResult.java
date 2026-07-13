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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_results")
@CompoundIndexes({
    @CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "pr_user_created_idx", def = "{'pullRequestId': 1, 'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "repo_user_created_idx", def = "{'repositoryId': 1, 'userId': 1, 'createdAt': -1}")
})
public class AnalysisResult {

    @Id
    private String id;

    @Indexed
    private String analysisRequestId;

    @Indexed
    private String pullRequestId;

    @Indexed
    private String repositoryId;

    @Indexed
    private String userId;

    private String status; // completed, failed

    private String summary;

    private Map<String, Object> findings;

    private Double score;

    private String model;

    private String modelVersion;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private Map<String, Object> rawOutput;
}
