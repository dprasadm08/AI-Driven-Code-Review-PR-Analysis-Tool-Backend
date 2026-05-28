package com.aiprreview.repository;

import com.aiprreview.entity.AnalysisRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRequestRepository extends MongoRepository<AnalysisRequest, String> {

    List<AnalysisRequest> findByStatus(String status);

    List<AnalysisRequest> findByPullRequestId(String pullRequestId);

    List<AnalysisRequest> findByUserId(String userId);
}
