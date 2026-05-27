package com.aiprreview.repository;

import com.aiprreview.entity.WebhookEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends MongoRepository<WebhookEvent, String> {

    List<WebhookEvent> findByRepositoryFullName(String repositoryFullName);
    
    List<WebhookEvent> findByRepositoryFullNameOrderByCreatedAtDesc(String repositoryFullName);
    
    List<WebhookEvent> findByEventType(String eventType);
    
    List<WebhookEvent> findByEventTypeAndAction(String eventType, String action);
    
    List<WebhookEvent> findByStatus(String status);
    
    Optional<WebhookEvent> findByRepositoryIdAndPullRequestNumber(Long repositoryId, Integer pullRequestNumber);
    
    List<WebhookEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByStatus(String status);
    
    long countByEventType(String eventType);
}
