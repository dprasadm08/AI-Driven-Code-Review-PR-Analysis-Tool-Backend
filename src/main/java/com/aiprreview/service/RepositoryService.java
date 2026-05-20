package com.aiprreview.service;

import com.aiprreview.dto.repository.RepositoryRequest;
import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.entity.User;
import com.aiprreview.exception.ResourceNotFoundException;
import com.aiprreview.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final AuthService authService;

    @Transactional
    public RepositoryResponse addRepository(RepositoryRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // Check if repository already exists for this user
        if (repositoryRepository.existsByUserIdAndFullName(currentUser.getId(), request.getFullName())) {
            throw new IllegalArgumentException("Repository '" + request.getFullName() + "' already exists");
        }

        // Parse owner and name from fullName
        String[] parts = request.getFullName().split("/");
        String owner = parts[0];
        String name = parts[1];

        // Create repository entity
        RepositoryEntity repository = RepositoryEntity.builder()
                .userId(currentUser.getId())
                .name(name)
                .fullName(request.getFullName())
                .owner(owner)
                .url("https://github.com/" + request.getFullName())
                .description(request.getDescription())
                .defaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main")
                .isPrivate(false)
                .isActive(true)
                .webhookEnabled(request.getEnableWebhook() != null ? request.getEnableWebhook() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RepositoryEntity savedRepository = repositoryRepository.save(repository);
        log.info("Repository added: {} by user: {}", savedRepository.getFullName(), currentUser.getUsername());

        return mapToResponse(savedRepository);
    }

    public List<RepositoryResponse> getAllRepositories() {
        User currentUser = authService.getCurrentUser();
        List<RepositoryEntity> repositories = repositoryRepository.findByUserId(currentUser.getId());
        log.info("Fetched {} repositories for user: {}", repositories.size(), currentUser.getUsername());
        return repositories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RepositoryResponse> getActiveRepositories() {
        User currentUser = authService.getCurrentUser();
        List<RepositoryEntity> repositories = repositoryRepository.findByUserIdAndIsActive(
                currentUser.getId(), true);
        return repositories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RepositoryResponse getRepositoryById(String id) {
        User currentUser = authService.getCurrentUser();
        RepositoryEntity repository = repositoryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + id));
        log.info("Fetched repository: {} for user: {}", repository.getFullName(), currentUser.getUsername());
        return mapToResponse(repository);
    }

    public RepositoryResponse getRepositoryByFullName(String fullName) {
        User currentUser = authService.getCurrentUser();
        RepositoryEntity repository = repositoryRepository.findByUserIdAndFullName(
                currentUser.getId(), fullName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Repository not found: " + fullName));
        return mapToResponse(repository);
    }

    @Transactional
    public RepositoryResponse updateRepository(String id, RepositoryRequest request) {
        User currentUser = authService.getCurrentUser();
        RepositoryEntity repository = repositoryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + id));

        // Update fields
        if (request.getDescription() != null) {
            repository.setDescription(request.getDescription());
        }
        if (request.getDefaultBranch() != null) {
            repository.setDefaultBranch(request.getDefaultBranch());
        }
        if (request.getEnableWebhook() != null) {
            repository.setWebhookEnabled(request.getEnableWebhook());
        }
        repository.setUpdatedAt(LocalDateTime.now());

        RepositoryEntity updatedRepository = repositoryRepository.save(repository);
        log.info("Repository updated: {} by user: {}", updatedRepository.getFullName(), currentUser.getUsername());

        return mapToResponse(updatedRepository);
    }

    @Transactional
    public void deleteRepository(String id) {
        User currentUser = authService.getCurrentUser();
        RepositoryEntity repository = repositoryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + id));

        repositoryRepository.delete(repository);
        log.info("Repository deleted: {} by user: {}", repository.getFullName(), currentUser.getUsername());
    }

    @Transactional
    public RepositoryResponse toggleRepositoryStatus(String id) {
        User currentUser = authService.getCurrentUser();
        RepositoryEntity repository = repositoryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + id));

        repository.setIsActive(!repository.getIsActive());
        repository.setUpdatedAt(LocalDateTime.now());

        RepositoryEntity updatedRepository = repositoryRepository.save(repository);
        log.info("Repository status toggled: {} to {} by user: {}", 
                updatedRepository.getFullName(), updatedRepository.getIsActive(), currentUser.getUsername());

        return mapToResponse(updatedRepository);
    }

    public Long getRepositoryCount() {
        User currentUser = authService.getCurrentUser();
        return repositoryRepository.countByUserId(currentUser.getId());
    }

    public List<RepositoryResponse> searchRepositories(String query) {
        User currentUser = authService.getCurrentUser();
        List<RepositoryEntity> repositories = repositoryRepository
                .findByUserIdAndNameContainingIgnoreCase(currentUser.getId(), query);
        return repositories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
