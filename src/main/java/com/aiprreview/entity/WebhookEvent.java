package com.aiprreview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "webhook_events")
public class WebhookEvent {

    @Id
    private String id;

    @Indexed
    private String eventType; // pull_request, push, issues, etc.

    private String action; // opened, closed, synchronize, etc.

    @Indexed
    private String repositoryFullName; // owner/repo

    @Indexed
    private Long repositoryId;

    @Indexed
    private Integer pullRequestNumber;

    private Long pullRequestId;

    private String payload; // JSON payload from GitHub

    @Indexed
    private String status; // received, processing, processed, failed

    private String errorMessage;

    private String sender; // GitHub username who triggered the event

    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @Indexed
    private LocalDateTime createdAt;
}
