package com.aiprreview.repository;

import com.aiprreview.entity.PullRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PullRequestRepository extends MongoRepository<PullRequest, String> {

    // Find by repository
    List<PullRequest> findByRepositoryId(String repositoryId);
    
    List<PullRequest> findByRepositoryIdOrderByCreatedAtDesc(String repositoryId);
    
    // Find by user
    List<PullRequest> findByUserId(String userId);
    
    List<PullRequest> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Find by repository and user
    List<PullRequest> findByRepositoryIdAndUserId(String repositoryId, String userId);
    
    // Find by PR number
    Optional<PullRequest> findByRepositoryIdAndPrNumber(String repositoryId, Integer prNumber);

    Optional<PullRequest> findByIdAndUserId(String id, String userId);
    
    // Find by state
    List<PullRequest> findByRepositoryIdAndState(String repositoryId, String state);
    
    List<PullRequest> findByUserIdAndState(String userId, String state);
    
    // Find by analysis status
    List<PullRequest> findByAnalysisStatus(String analysisStatus);
    
    List<PullRequest> findByRepositoryIdAndAnalysisStatus(String repositoryId, String analysisStatus);
    
    // Count methods
    long countByRepositoryId(String repositoryId);
    
    long countByUserId(String userId);
    
    long countByRepositoryIdAndState(String repositoryId, String state);
    
    long countByUserIdAndState(String userId, String state);
    
    // Check existence
    boolean existsByRepositoryIdAndPrNumber(String repositoryId, Integer prNumber);
}
