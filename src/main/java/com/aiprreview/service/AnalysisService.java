package com.aiprreview.service;

import com.aiprreview.ai.PromptBuilderService;
import com.aiprreview.entity.AnalysisRequest;
import com.aiprreview.entity.AnalysisResult;
import com.aiprreview.entity.PullRequest;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.repository.AnalysisRequestRepository;
import com.aiprreview.repository.AnalysisResultRepository;
import com.aiprreview.repository.PullRequestRepository;
import com.aiprreview.repository.RepositoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalysisService {

	private final AnalysisRequestRepository requestRepository;
	private final AnalysisResultRepository resultRepository;
	private final PullRequestRepository pullRequestRepository;
	private final RepositoryRepository repositoryRepository;
	private final PromptBuilderService promptBuilderService;

	public AnalysisService(
			AnalysisRequestRepository requestRepository,
			AnalysisResultRepository resultRepository,
			PullRequestRepository pullRequestRepository,
			RepositoryRepository repositoryRepository,
			PromptBuilderService promptBuilderService) {
		this.requestRepository = requestRepository;
		this.resultRepository = resultRepository;
		this.pullRequestRepository = pullRequestRepository;
		this.repositoryRepository = repositoryRepository;
		this.promptBuilderService = promptBuilderService;
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

	public PromptBuilderService.PromptBuildResult buildPromptForRequest(String requestId) {
		AnalysisRequest request = requestRepository.findById(requestId)
				.orElseThrow(() -> new IllegalArgumentException("Analysis request not found: " + requestId));

		PullRequest pullRequest = pullRequestRepository.findById(request.getPullRequestId())
				.orElseThrow(() -> new IllegalArgumentException("Pull request not found: " + request.getPullRequestId()));

		RepositoryEntity repository = repositoryRepository.findById(pullRequest.getRepositoryId())
				.orElseThrow(() -> new IllegalArgumentException("Repository not found: " + pullRequest.getRepositoryId()));

		String diffInput = request.getMetadata() != null
				? String.valueOf(request.getMetadata().getOrDefault("diff", ""))
				: "";

		PullRequestWithFilesResponse prContext = PullRequestWithFilesResponse.builder()
				.id(pullRequest.getId())
				.repositoryId(repository.getId())
				.repositoryName(repository.getName())
				.repositoryFullName(repository.getFullName())
				.prNumber(pullRequest.getPrNumber())
				.title(pullRequest.getTitle())
				.description(pullRequest.getDescription())
				.state(pullRequest.getState())
				.author(pullRequest.getAuthor())
				.baseBranch(pullRequest.getBaseBranch())
				.headBranch(pullRequest.getHeadBranch())
				.files(null)
				.diff(diffInput)
				.build();

		return promptBuilderService.buildPrompt(prContext);
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

			PromptBuilderService.PromptBuildResult promptBuildResult = buildPromptForRequest(req.getId());
			Map<String, Object> metadata = req.getMetadata() != null ? req.getMetadata() : new HashMap<>();
			metadata.put("aiPrompt", promptBuildResult.prompt());
			metadata.put("aiPromptEstimatedTokens", promptBuildResult.estimatedTokens());
			metadata.put("aiPromptTruncated", promptBuildResult.truncated());
			metadata.put("aiPromptOmittedChars", promptBuildResult.omittedChars());
			req.setMetadata(metadata);
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
