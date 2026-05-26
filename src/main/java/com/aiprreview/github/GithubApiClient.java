package com.aiprreview.github;

import com.aiprreview.dto.github.GithubCommitDto;
import com.aiprreview.dto.github.GithubPullRequestDto;
import com.aiprreview.dto.github.GithubPullRequestFileDto;
import com.aiprreview.dto.github.GithubRepositoryDto;
import com.aiprreview.exception.GithubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubApiClient {

    private final WebClient githubWebClient;

    @Value("${app.github.api.token:}")
    private String defaultGithubToken;

    @Value("${app.github.api.per-page:30}")
    private int perPage;

    /**
     * Fetch all repositories for authenticated user
     */
    public List<GithubRepositoryDto> getUserRepositories(String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;
        
        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch repositories");
        }

        try {
            return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/repos")
                            .queryParam("per_page", perPage)
                            .queryParam("sort", "updated")
                            .queryParam("affiliation", "owner,collaborator")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API 4xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API client error: " + response.statusCode()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<GithubRepositoryDto>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch GitHub repositories", ex);
            throw new GithubApiException("Failed to fetch repositories from GitHub: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch repositories for a specific user
     */
    public List<GithubRepositoryDto> getUserRepositoriesByUsername(String username, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        try {
            WebClient.RequestHeadersSpec<?> spec = githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/{username}/repos")
                            .queryParam("per_page", perPage)
                            .queryParam("sort", "updated")
                            .build(username))
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

            // Add auth token if provided
            if (authToken != null && !authToken.isEmpty()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
            }

            return spec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API 4xx error for user {}: {}", username, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API error for user " + username + ": " + response.statusCode()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<GithubRepositoryDto>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch GitHub repositories for user: {}", username, ex);
            throw new GithubApiException("Failed to fetch repositories for user " + username + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch a single repository
     */
    public GithubRepositoryDto getRepository(String owner, String repo, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        try {
            WebClient.RequestHeadersSpec<?> spec = githubWebClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

            if (authToken != null && !authToken.isEmpty()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
            }

            return spec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching repo {}/{}: {}", owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Repository not found: " + owner + "/" + repo));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(GithubRepositoryDto.class)
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch repository {}/{}", owner, repo, ex);
            throw new GithubApiException("Failed to fetch repository: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch pull requests for a repository
     */
    public List<GithubPullRequestDto> getRepositoryPullRequests(String owner, String repo, String state, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull requests");
        }

        try {
            return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls")
                            .queryParam("state", state != null ? state : "all")
                            .queryParam("per_page", perPage)
                            .queryParam("sort", "updated")
                            .queryParam("direction", "desc")
                            .build(owner, repo))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching PRs for {}/{}: {}", owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Failed to fetch pull requests for " + owner + "/" + repo));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<GithubPullRequestDto>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch pull requests for {}/{}", owner, repo, ex);
            throw new GithubApiException("Failed to fetch pull requests: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch a single pull request
     */
    public GithubPullRequestDto getPullRequest(String owner, String repo, Integer prNumber, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull request");
        }

        try {
            return githubWebClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching PR #{} for {}/{}: {}", 
                                prNumber, owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Pull request #" + prNumber + " not found"));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(GithubPullRequestDto.class)
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch pull request #{} for {}/{}", prNumber, owner, repo, ex);
            throw new GithubApiException("Failed to fetch pull request: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch files changed in a pull request
     */
    public List<GithubPullRequestFileDto> getPullRequestFiles(String owner, String repo, Integer prNumber, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull request files");
        }

        try {
            return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls/{prNumber}/files")
                            .queryParam("per_page", 100) // GitHub allows up to 100 files per page
                            .build(owner, repo, prNumber))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching files for PR #{} in {}/{}: {}", 
                                prNumber, owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Failed to fetch files for pull request #" + prNumber));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<GithubPullRequestFileDto>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch files for PR #{} in {}/{}", prNumber, owner, repo, ex);
            throw new GithubApiException("Failed to fetch pull request files: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch commits in a pull request
     */
    public List<GithubCommitDto> getPullRequestCommits(String owner, String repo, Integer prNumber, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull request commits");
        }

        try {
            return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls/{prNumber}/commits")
                            .queryParam("per_page", 100)
                            .build(owner, repo, prNumber))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching commits for PR #{} in {}/{}: {}", 
                                prNumber, owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Failed to fetch commits for pull request #" + prNumber));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<GithubCommitDto>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch commits for PR #{} in {}/{}", prNumber, owner, repo, ex);
            throw new GithubApiException("Failed to fetch pull request commits: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch the unified diff for a pull request
     */
    public String getPullRequestDiff(String owner, String repo, Integer prNumber, String token) {
        String authToken = token != null && !token.isEmpty() ? token : defaultGithubToken;

        if (authToken == null || authToken.isEmpty()) {
            throw new GithubApiException("GitHub token is required to fetch pull request diff");
        }

        try {
            return githubWebClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("GitHub API error fetching diff for PR #{} in {}/{}: {}", 
                                prNumber, owner, repo, response.statusCode());
                        return Mono.error(new GithubApiException(
                                "Failed to fetch diff for pull request #" + prNumber));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error: {}", response.statusCode());
                        return Mono.error(new GithubApiException(
                                "GitHub API server error: " + response.statusCode()));
                    })
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch diff for PR #{} in {}/{}", prNumber, owner, repo, ex);
            throw new GithubApiException("Failed to fetch pull request diff: " + ex.getMessage(), ex);
        }
    }
}
