package com.aiprreview.service;

import com.aiprreview.entity.AnalysisRequest;
import com.aiprreview.entity.AnalysisResult;
import com.aiprreview.repository.AnalysisRequestRepository;
import com.aiprreview.repository.AnalysisResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

	private final AnalysisRequestRepository requestRepository;
	private final AnalysisResultRepository resultRepository;

	public AnalysisService(AnalysisRequestRepository requestRepository, AnalysisResultRepository resultRepository) {
		this.requestRepository = requestRepository;
		this.resultRepository = resultRepository;
	}

	public AnalysisRequest submitAnalysisRequest(AnalysisRequest req) {
		if (req.getStatus() == null) req.setStatus("queued");
		req.setCreatedAt(LocalDateTime.now());
		return requestRepository.save(req);
	}

	public Optional<AnalysisRequest> getAnalysisRequestById(String id) {
		return requestRepository.findById(id);
	}

	public List<AnalysisRequest> getPendingRequests() {
		return requestRepository.findByStatus("queued");
	}

	public AnalysisResult saveAnalysisResult(AnalysisResult result) {
		if (result.getCreatedAt() == null) result.setCreatedAt(LocalDateTime.now());
		AnalysisResult saved = resultRepository.save(result);
		if (saved.getAnalysisRequestId() != null) {
			requestRepository.findById(saved.getAnalysisRequestId()).ifPresent(r -> {
				r.setStatus(saved.getStatus() != null ? saved.getStatus() : "completed");
				r.setResultId(saved.getId());
				r.setCompletedAt(saved.getCompletedAt() != null ? saved.getCompletedAt() : LocalDateTime.now());
				requestRepository.save(r);
			});
		}
		return saved;
	}

	public Optional<AnalysisResult> getAnalysisResultById(String id) {
		return resultRepository.findById(id);
	}

	public List<AnalysisResult> getResultsForPullRequest(String pullRequestId) {
		return resultRepository.findByPullRequestId(pullRequestId);
	}

	/**
	 * Simple processor that picks up queued requests and marks them completed with a placeholder result.
	 * Replace with integration to an AI analysis provider when ready.
	 */
	public void processPendingRequests() {
		List<AnalysisRequest> pending = getPendingRequests();
		for (AnalysisRequest req : pending) {
			req.setStatus("in_progress");
			req.setStartedAt(LocalDateTime.now());
			requestRepository.save(req);

			// TODO: integrate with AI provider to perform real analysis
			AnalysisResult res = AnalysisResult.builder()
					.analysisRequestId(req.getId())
					.pullRequestId(req.getPullRequestId())
					.repositoryId(req.getRepositoryId())
					.userId(req.getUserId())
					.status("completed")
					.summary("Analysis not implemented yet")
					.createdAt(LocalDateTime.now())
					.completedAt(LocalDateTime.now())
					.build();

			saveAnalysisResult(res);
		}
	}
}
