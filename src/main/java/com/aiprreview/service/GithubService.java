package com.aiprreview.service;

import com.aiprreview.dto.github.GithubRepositoryDto;
import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.entity.User;
import com.aiprreview.exception.GithubApiException;
import com.aiprreview.github.GithubApiClient;
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
public class GithubService {

    private final GithubApiClient githubApiClient;
    private final RepositoryRepository repositoryRepository;
    private final AuthService authService;

    /**
     * Fetch and sync repositories from GitHub for current user
     */
    @Transactional
    public List<RepositoryResponse> syncUserRepositories(String githubToken) {
        User currentUser = authService.getCurrentUser();
        String token = githubToken != null ? githubToken : currentUser.getGithubToken();

        if (token == null || token.isEmpty()) {
            throw new GithubApiException("GitHub token is required. Please provide a token or set it in your profile.");
        }

        log.info("Fetching repositories from GitHub for user: {}", currentUser.getUsername());
        
        try {
            // Fetch repositories from GitHub
            List<GithubRepositoryDto> githubRepos = githubApiClient.getUserRepositories(token);
            log.info("Found {} repositories from GitHub", githubRepos.size());

            // Save or update repositories
            List<RepositoryEntity> savedRepos = new ArrayList<>();
            for (GithubRepositoryDto githubRepo : githubRepos) {
                RepositoryEntity repo = syncRepository(currentUser.getId(), githubRepo);
                savedRepos.add(repo);
            }

            log.info("Synced {} repositories for user: {}", savedRepos.size(), currentUser.getUsername());

            // Convert to response DTOs
            return savedRepos.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
                    
        } catch (GithubApiException ex) {
            log.error("GitHub API error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to sync repositories from GitHub", ex);
            throw new GithubApiException("Failed to sync repositories: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch a specific repository from GitHub and save it
     */
    @Transactional
    public RepositoryResponse fetchAndSaveRepository(String fullName, String githubToken) {
        User currentUser = authService.getCurrentUser();
        String token = githubToken != null ? githubToken : currentUser.getGithubToken();

        String[] parts = fullName.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository fullName must be in format 'owner/repo'");
        }

        String owner = parts[0];
        String repo = parts[1];

        log.info("Fetching repository {}/{} from GitHub", owner, repo);

        try {
            // Fetch from GitHub
            GithubRepositoryDto githubRepo = githubApiClient.getRepository(owner, repo, token);
            
            // Save or update
            RepositoryEntity savedRepo = syncRepository(currentUser.getId(), githubRepo);
            
            log.info("Successfully fetched and saved repository: {}", fullName);
            return mapToResponse(savedRepo);
            
        } catch (GithubApiException ex) {
            log.error("GitHub API error fetching {}/{}: {}", owner, repo, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch repository {}/{}", owner, repo, ex);
            throw new GithubApiException("Failed to fetch repository: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sync a single repository (create or update)
     */
    private RepositoryEntity syncRepository(String userId, GithubRepositoryDto githubRepo) {
        // Check if repository already exists
        RepositoryEntity existingRepo = repositoryRepository
                .findByUserIdAndFullName(userId, githubRepo.getFullName())
                .orElse(null);

        if (existingRepo != null) {
            // Update existing repository
            return updateRepositoryFromGithub(existingRepo, githubRepo);
        } else {
            // Create new repository
            return createRepositoryFromGithub(userId, githubRepo);
        }
    }

    /**
     * Create new repository entity from GitHub data
     */
    private RepositoryEntity createRepositoryFromGithub(String userId, GithubRepositoryDto githubRepo) {
        RepositoryEntity repository = RepositoryEntity.builder()
                .userId(userId)
                .name(githubRepo.getName())
                .fullName(githubRepo.getFullName())
                .owner(githubRepo.getOwner().getLogin())
                .url(githubRepo.getHtmlUrl())
                .description(githubRepo.getDescription())
                .defaultBranch(githubRepo.getDefaultBranch())
                .language(githubRepo.getLanguage())
                .isPrivate(githubRepo.getIsPrivate())
                .isActive(true)
                .githubId(githubRepo.getId())
                .stars(githubRepo.getStars())
                .forks(githubRepo.getForks())
                .openIssues(githubRepo.getOpenIssues())
                .webhookEnabled(false)
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repositoryRepository.save(repository);
    }

    /**
     * Update existing repository with GitHub data
     */
    private RepositoryEntity updateRepositoryFromGithub(RepositoryEntity repository, GithubRepositoryDto githubRepo) {
        repository.setDescription(githubRepo.getDescription());
        repository.setDefaultBranch(githubRepo.getDefaultBranch());
        repository.setLanguage(githubRepo.getLanguage());
        repository.setIsPrivate(githubRepo.getIsPrivate());
        repository.setStars(githubRepo.getStars());
        repository.setForks(githubRepo.getForks());
        repository.setOpenIssues(githubRepo.getOpenIssues());
        repository.setLastSyncedAt(LocalDateTime.now());
        repository.setUpdatedAt(LocalDateTime.now());

        return repositoryRepository.save(repository);
    }

    private RepositoryResponse mapToResponse(RepositoryEntity repository) {
        return RepositoryResponse.builder()
                .id(repository.getId())
                .name(repository.getName())
                .fullName(repository.getFullName())
                .owner(repository.getOwner())
                .url(repository.getUrl())
                .description(repository.getDescription())
                .defaultBranch(repository.getDefaultBranch())
                .language(repository.getLanguage())
                .isPrivate(repository.getIsPrivate())
                .isActive(repository.getIsActive())
                .githubId(repository.getGithubId())
                .stars(repository.getStars())
                .forks(repository.getForks())
                .openIssues(repository.getOpenIssues())
                .webhookEnabled(repository.getWebhookEnabled())
                .lastSyncedAt(repository.getLastSyncedAt())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .build();
    }
}
