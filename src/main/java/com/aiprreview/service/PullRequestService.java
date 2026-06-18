package com.aiprreview.service;

import com.aiprreview.dto.github.GithubCommitDto;
import com.aiprreview.dto.github.GithubPullRequestDto;
import com.aiprreview.dto.github.GithubPullRequestFileDto;
import com.aiprreview.dto.pullrequest.PullRequestCommitResponse;
import com.aiprreview.dto.pullrequest.PullRequestDetailResponse;
import com.aiprreview.dto.pullrequest.PullRequestFileResponse;
import com.aiprreview.dto.pullrequest.PullRequestResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.entity.PullRequest;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.entity.User;
import com.aiprreview.exception.GithubApiException;
import com.aiprreview.exception.ResourceNotFoundException;
import com.aiprreview.github.GithubApiClient;
import org.springframework.security.access.AccessDeniedException;
import com.aiprreview.repository.PullRequestRepository;
import com.aiprreview.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubApiClient githubApiClient;
    private final AuthService authService;

    /**
     * Sync pull requests for a repository from GitHub
     */
    @Transactional
    public List<PullRequestResponse> syncRepositoryPullRequests(String repositoryId, String state, String githubToken) {
        User currentUser = authService.getCurrentUser();
        
        // Get repository
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));
        
        // Verify ownership
        if (!repository.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this repository");
        }
        
        String token = githubToken != null ? githubToken : currentUser.getGithubToken();
        
        if (token == null || token.isEmpty()) {
            throw new GithubApiException("GitHub token is required. Please provide a token or set it in your profile.");
        }
        
        log.info("Syncing pull requests for repository: {}", repository.getFullName());
        
        try {
            // Fetch PRs from GitHub
            List<GithubPullRequestDto> githubPRs = githubApiClient.getRepositoryPullRequests(
                    repository.getOwner(), 
                    repository.getName(), 
                    state, 
                    token
            );
            
            log.info("Found {} pull requests from GitHub for {}", githubPRs.size(), repository.getFullName());
            
            // Save or update PRs
            List<PullRequest> savedPRs = new ArrayList<>();
            for (GithubPullRequestDto githubPR : githubPRs) {
                PullRequest pr = syncPullRequest(repository, currentUser.getId(), githubPR);
                savedPRs.add(pr);
            }
            
            log.info("Synced {} pull requests for repository: {}", savedPRs.size(), repository.getFullName());
            
            return savedPRs.stream()
                    .map(pr -> mapToResponse(pr, repository.getName()))
                    .collect(Collectors.toList());
                    
        } catch (GithubApiException ex) {
            log.error("GitHub API error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to sync pull requests", ex);
            throw new GithubApiException("Failed to sync pull requests: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch and save a single pull request from GitHub
     */
    @Transactional
    public PullRequestDetailResponse fetchPullRequest(String repositoryId, Integer prNumber, String githubToken) {
        User currentUser = authService.getCurrentUser();
        
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));
        
        if (!repository.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this repository");
        }
        
        String token = githubToken != null ? githubToken : currentUser.getGithubToken();
        
        log.info("Fetching PR #{} for repository: {}", prNumber, repository.getFullName());
        
        try {
            GithubPullRequestDto githubPR = githubApiClient.getPullRequest(
                    repository.getOwner(),
                    repository.getName(),
                    prNumber,
                    token
            );
            
            PullRequest pr = syncPullRequest(repository, currentUser.getId(), githubPR);
            
            log.info("Successfully fetched and saved PR #{}", prNumber);
            return mapToDetailResponse(pr, repository);
            
        } catch (GithubApiException ex) {
            log.error("GitHub API error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch pull request", ex);
            throw new GithubApiException("Failed to fetch pull request: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get all pull requests for current user
     */
    public List<PullRequestResponse> getAllPullRequests(String state) {
        User currentUser = authService.getCurrentUser();
        
        List<PullRequest> pullRequests;
        if (state != null && !state.isEmpty()) {
            pullRequests = pullRequestRepository.findByUserIdAndState(currentUser.getId(), state);
        } else {
            pullRequests = pullRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        }
        
        return pullRequests.stream()
                .map(pr -> {
                    RepositoryEntity repo = repositoryRepository.findById(pr.getRepositoryId()).orElse(null);
                    return mapToResponse(pr, repo != null ? repo.getName() : "Unknown");
                })
                .collect(Collectors.toList());
    }

    /**
     * Get pull requests for a specific repository
     */
    public List<PullRequestResponse> getRepositoryPullRequests(String repositoryId, String state) {
        User currentUser = authService.getCurrentUser();
        
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));
        
        if (!repository.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this repository");
        }
        
        List<PullRequest> pullRequests;
        if (state != null && !state.isEmpty()) {
            pullRequests = pullRequestRepository.findByRepositoryIdAndState(repositoryId, state);
        } else {
            pullRequests = pullRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
        }
        
        return pullRequests.stream()
                .map(pr -> mapToResponse(pr, repository.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Get pull request by ID
     */
    public PullRequestDetailResponse getPullRequestById(String id) {
        User currentUser = authService.getCurrentUser();
        
        PullRequest pullRequest = pullRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + id));
        
        if (!pullRequest.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this pull request");
        }
        
        RepositoryEntity repository = repositoryRepository.findById(pullRequest.getRepositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        
        return mapToDetailResponse(pullRequest, repository);
    }

    /**
     * Get pull request details with files and commits from GitHub
     */
    public PullRequestWithFilesResponse getPullRequestWithFiles(String id, String githubToken, boolean includeDiff) {
        User currentUser = authService.getCurrentUser();
        
        PullRequest pullRequest = pullRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + id));
        
        if (!pullRequest.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this pull request");
        }
        
        RepositoryEntity repository = repositoryRepository.findById(pullRequest.getRepositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        
        String token = githubToken != null ? githubToken : currentUser.getGithubToken();
        
        if (token == null || token.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull request details");
        }
        
        log.info("Fetching PR #{} details with files from GitHub for repository: {}", 
                pullRequest.getPrNumber(), repository.getFullName());
        
        try {
            // Fetch files
            List<GithubPullRequestFileDto> githubFiles = githubApiClient.getPullRequestFiles(
                    repository.getOwner(),
                    repository.getName(),
                    pullRequest.getPrNumber(),
                    token
            );
            
            // Fetch commits
            List<GithubCommitDto> githubCommits = githubApiClient.getPullRequestCommits(
                    repository.getOwner(),
                    repository.getName(),
                    pullRequest.getPrNumber(),
                    token
            );
            
            // Optionally fetch diff
            String diff = null;
            if (includeDiff) {
                diff = githubApiClient.getPullRequestDiff(
                        repository.getOwner(),
                        repository.getName(),
                        pullRequest.getPrNumber(),
                        token
                );
            }
            
            // Map to response DTOs
            List<PullRequestFileResponse> files = githubFiles.stream()
                    .map(this::mapToFileResponse)
                    .collect(Collectors.toList());
            
            List<PullRequestCommitResponse> commits = githubCommits.stream()
                    .map(this::mapToCommitResponse)
                    .collect(Collectors.toList());
            
            log.info("Successfully fetched {} files and {} commits for PR #{}", 
                    files.size(), commits.size(), pullRequest.getPrNumber());
            
            return mapToWithFilesResponse(pullRequest, repository, files, commits, diff);
            
        } catch (GithubApiException ex) {
            log.error("GitHub API error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch PR details with files", ex);
            throw new GithubApiException("Failed to fetch pull request details: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get pull request count for user
     */
    public long getPullRequestCount(String state) {
        User currentUser = authService.getCurrentUser();
        
        if (state != null && !state.isEmpty()) {
            return pullRequestRepository.countByUserIdAndState(currentUser.getId(), state);
        }
        return pullRequestRepository.countByUserId(currentUser.getId());
    }

    /**
     * Get pull request count for repository
     */
    public long getRepositoryPullRequestCount(String repositoryId, String state) {
        User currentUser = authService.getCurrentUser();
        
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));
        
        if (!repository.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this repository");
        }
        
        if (state != null && !state.isEmpty()) {
            return pullRequestRepository.countByRepositoryIdAndState(repositoryId, state);
        }
        return pullRequestRepository.countByRepositoryId(repositoryId);
    }

    /**
     * Sync a single pull request (create or update)
     */
    private PullRequest syncPullRequest(RepositoryEntity repository, String userId, GithubPullRequestDto githubPR) {
        PullRequest existingPR = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repository.getId(), githubPR.getNumber())
                .orElse(null);
        
        if (existingPR != null) {
            return updatePullRequestFromGithub(existingPR, githubPR);
        } else {
            return createPullRequestFromGithub(repository, userId, githubPR);
        }
    }

    /**
     * Create new pull request from GitHub data
     */
    private PullRequest createPullRequestFromGithub(RepositoryEntity repository, String userId, GithubPullRequestDto githubPR) {
        // Determine state (open, closed, merged)
        String state;
        if (Boolean.TRUE.equals(githubPR.getMerged())) {
            state = "merged";
        } else {
            state = githubPR.getState(); // open or closed
        }
        
        // Extract labels
        List<String> labels = new ArrayList<>();
        if (githubPR.getLabels() != null) {
            labels = githubPR.getLabels().stream()
                    .map(GithubPullRequestDto.LabelDto::getName)
                    .collect(Collectors.toList());
        }
        
        PullRequest pullRequest = PullRequest.builder()
                .repositoryId(repository.getId())
                .userId(userId)
                .githubId(githubPR.getId())
                .prNumber(githubPR.getNumber())
                .title(githubPR.getTitle())
                .description(githubPR.getDescription())
                .state(state)
                .author(githubPR.getUser() != null ? githubPR.getUser().getLogin() : "unknown")
                .authorAvatarUrl(githubPR.getUser() != null ? githubPR.getUser().getAvatarUrl() : null)
                .htmlUrl(githubPR.getHtmlUrl())
                .headBranch(githubPR.getHead() != null ? githubPR.getHead().getRef() : null)
                .headSha(githubPR.getHead() != null ? githubPR.getHead().getSha() : null)
                .baseBranch(githubPR.getBase() != null ? githubPR.getBase().getRef() : null)
                .baseSha(githubPR.getBase() != null ? githubPR.getBase().getSha() : null)
                .isDraft(githubPR.getDraft())
                .isMerged(githubPR.getMerged())
                .isMergeable(githubPR.getMergeable())
                .mergeableState(githubPR.getMergeableState())
                .mergedBy(githubPR.getMergedBy() != null ? githubPR.getMergedBy().getLogin() : null)
                .mergedAt(githubPR.getMergedAt())
                .commentsCount(githubPR.getComments())
                .reviewCommentsCount(githubPR.getReviewComments())
                .commitsCount(githubPR.getCommits())
                .additions(githubPR.getAdditions())
                .deletions(githubPR.getDeletions())
                .changedFiles(githubPR.getChangedFiles())
                .labels(labels)
                .analysisStatus("pending")
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(githubPR.getCreatedAt())
                .updatedAt(githubPR.getUpdatedAt())
                .closedAt(githubPR.getClosedAt())
                .build();
        
        return pullRequestRepository.save(pullRequest);
    }

    /**
     * Update existing pull request with GitHub data
     */
    private PullRequest updatePullRequestFromGithub(PullRequest pullRequest, GithubPullRequestDto githubPR) {
        // Determine state
        String state;
        if (Boolean.TRUE.equals(githubPR.getMerged())) {
            state = "merged";
        } else {
            state = githubPR.getState();
        }
        
        // Extract labels
        List<String> labels = new ArrayList<>();
        if (githubPR.getLabels() != null) {
            labels = githubPR.getLabels().stream()
                    .map(GithubPullRequestDto.LabelDto::getName)
                    .collect(Collectors.toList());
        }
        
        pullRequest.setTitle(githubPR.getTitle());
        pullRequest.setDescription(githubPR.getDescription());
        pullRequest.setState(state);
        pullRequest.setHeadSha(githubPR.getHead() != null ? githubPR.getHead().getSha() : null);
        pullRequest.setBaseSha(githubPR.getBase() != null ? githubPR.getBase().getSha() : null);
        pullRequest.setIsDraft(githubPR.getDraft());
        pullRequest.setIsMerged(githubPR.getMerged());
        pullRequest.setIsMergeable(githubPR.getMergeable());
        pullRequest.setMergeableState(githubPR.getMergeableState());
        pullRequest.setMergedBy(githubPR.getMergedBy() != null ? githubPR.getMergedBy().getLogin() : null);
        pullRequest.setMergedAt(githubPR.getMergedAt());
        pullRequest.setCommentsCount(githubPR.getComments());
        pullRequest.setReviewCommentsCount(githubPR.getReviewComments());
        pullRequest.setCommitsCount(githubPR.getCommits());
        pullRequest.setAdditions(githubPR.getAdditions());
        pullRequest.setDeletions(githubPR.getDeletions());
        pullRequest.setChangedFiles(githubPR.getChangedFiles());
        pullRequest.setLabels(labels);
        pullRequest.setLastSyncedAt(LocalDateTime.now());
        pullRequest.setUpdatedAt(githubPR.getUpdatedAt());
        pullRequest.setClosedAt(githubPR.getClosedAt());
        
        return pullRequestRepository.save(pullRequest);
    }

    private PullRequestResponse mapToResponse(PullRequest pr, String repositoryName) {
        return PullRequestResponse.builder()
                .id(pr.getId())
                .repositoryId(pr.getRepositoryId())
                .repositoryName(repositoryName)
                .githubId(pr.getGithubId())
                .prNumber(pr.getPrNumber())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .state(pr.getState())
                .author(pr.getAuthor())
                .authorAvatarUrl(pr.getAuthorAvatarUrl())
                .htmlUrl(pr.getHtmlUrl())
                .headBranch(pr.getHeadBranch())
                .baseBranch(pr.getBaseBranch())
                .isDraft(pr.getIsDraft())
                .isMerged(pr.getIsMerged())
                .isMergeable(pr.getIsMergeable())
                .commentsCount(pr.getCommentsCount())
                .reviewCommentsCount(pr.getReviewCommentsCount())
                .commitsCount(pr.getCommitsCount())
                .additions(pr.getAdditions())
                .deletions(pr.getDeletions())
                .changedFiles(pr.getChangedFiles())
                .labels(pr.getLabels())
                .analysisStatus(pr.getAnalysisStatus())
                .analysisResultId(pr.getAnalysisResultId())
                .analyzedAt(pr.getAnalyzedAt())
                .lastSyncedAt(pr.getLastSyncedAt())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .mergedAt(pr.getMergedAt())
                .closedAt(pr.getClosedAt())
                .build();
    }

    private PullRequestDetailResponse mapToDetailResponse(PullRequest pr, RepositoryEntity repository) {
        return PullRequestDetailResponse.builder()
                .id(pr.getId())
                .repositoryId(pr.getRepositoryId())
                .repositoryName(repository.getName())
                .repositoryFullName(repository.getFullName())
                .repositoryOwner(repository.getOwner())
                .githubId(pr.getGithubId())
                .prNumber(pr.getPrNumber())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .state(pr.getState())
                .author(pr.getAuthor())
                .authorAvatarUrl(pr.getAuthorAvatarUrl())
                .htmlUrl(pr.getHtmlUrl())
                .headBranch(pr.getHeadBranch())
                .headSha(pr.getHeadSha())
                .baseBranch(pr.getBaseBranch())
                .baseSha(pr.getBaseSha())
                .isDraft(pr.getIsDraft())
                .isMerged(pr.getIsMerged())
                .isMergeable(pr.getIsMergeable())
                .mergeableState(pr.getMergeableState())
                .mergedBy(pr.getMergedBy())
                .commentsCount(pr.getCommentsCount())
                .reviewCommentsCount(pr.getReviewCommentsCount())
                .commitsCount(pr.getCommitsCount())
                .additions(pr.getAdditions())
                .deletions(pr.getDeletions())
                .changedFiles(pr.getChangedFiles())
                .labels(pr.getLabels())
                .analysisStatus(pr.getAnalysisStatus())
                .analysisResultId(pr.getAnalysisResultId())
                .analyzedAt(pr.getAnalyzedAt())
                .lastSyncedAt(pr.getLastSyncedAt())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .mergedAt(pr.getMergedAt())
                .closedAt(pr.getClosedAt())
                .build();
    }

    private PullRequestFileResponse mapToFileResponse(GithubPullRequestFileDto file) {
        return PullRequestFileResponse.builder()
                .filename(file.getFilename())
                .status(file.getStatus())
                .additions(file.getAdditions())
                .deletions(file.getDeletions())
                .changes(file.getChanges())
                .patch(file.getPatch())
                .previousFilename(file.getPreviousFilename())
                .blobUrl(file.getBlobUrl())
                .rawUrl(file.getRawUrl())
                .build();
    }

    private PullRequestCommitResponse mapToCommitResponse(GithubCommitDto commit) {
        return PullRequestCommitResponse.builder()
                .sha(commit.getSha())
                .message(commit.getCommit() != null ? commit.getCommit().getMessage() : null)
                .author(commit.getAuthor() != null ? commit.getAuthor().getLogin() : null)
                .authorEmail(commit.getCommit() != null && commit.getCommit().getAuthor() != null 
                        ? commit.getCommit().getAuthor().getEmail() : null)
                .authorAvatarUrl(commit.getAuthor() != null ? commit.getAuthor().getAvatarUrl() : null)
                .authoredAt(commit.getCommit() != null && commit.getCommit().getAuthor() != null 
                        ? commit.getCommit().getAuthor().getDate() : null)
                .committer(commit.getCommitter() != null ? commit.getCommitter().getLogin() : null)
                .committerEmail(commit.getCommit() != null && commit.getCommit().getCommitter() != null 
                        ? commit.getCommit().getCommitter().getEmail() : null)
                .committedAt(commit.getCommit() != null && commit.getCommit().getCommitter() != null 
                        ? commit.getCommit().getCommitter().getDate() : null)
                .htmlUrl(commit.getHtmlUrl())
                .build();
    }

    private PullRequestWithFilesResponse mapToWithFilesResponse(
            PullRequest pr, 
            RepositoryEntity repository, 
            List<PullRequestFileResponse> files,
            List<PullRequestCommitResponse> commits,
            String diff) {
        return PullRequestWithFilesResponse.builder()
                .id(pr.getId())
                .repositoryId(pr.getRepositoryId())
                .repositoryName(repository.getName())
                .repositoryFullName(repository.getFullName())
                .prNumber(pr.getPrNumber())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .state(pr.getState())
                .author(pr.getAuthor())
                .authorAvatarUrl(pr.getAuthorAvatarUrl())
                .htmlUrl(pr.getHtmlUrl())
                .headBranch(pr.getHeadBranch())
                .headSha(pr.getHeadSha())
                .baseBranch(pr.getBaseBranch())
                .baseSha(pr.getBaseSha())
                .isDraft(pr.getIsDraft())
                .isMerged(pr.getIsMerged())
                .isMergeable(pr.getIsMergeable())
                .commentsCount(pr.getCommentsCount())
                .reviewCommentsCount(pr.getReviewCommentsCount())
                .commitsCount(pr.getCommitsCount())
                .additions(pr.getAdditions())
                .deletions(pr.getDeletions())
                .changedFiles(pr.getChangedFiles())
                .labels(pr.getLabels())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .mergedAt(pr.getMergedAt())
                .closedAt(pr.getClosedAt())
                .files(files)
                .commits(commits)
                .diff(diff)
                .build();
    }
}
