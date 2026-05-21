package com.aiprreview.github;

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
}
