package com.aiprreview.repository;

import com.aiprreview.entity.AnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends MongoRepository<AnalysisResult, String> {

    List<AnalysisResult> findByAnalysisRequestId(String analysisRequestId);

    List<AnalysisResult> findByPullRequestId(String pullRequestId);

    List<AnalysisResult> findByUserId(String userId);

        Page<AnalysisResult> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

        Page<AnalysisResult> findByPullRequestIdAndUserIdOrderByCreatedAtDesc(
            String pullRequestId,
            String userId,
            Pageable pageable
        );

        Page<AnalysisResult> findByRepositoryIdAndUserIdOrderByCreatedAtDesc(
            String repositoryId,
            String userId,
            Pageable pageable
        );
}
