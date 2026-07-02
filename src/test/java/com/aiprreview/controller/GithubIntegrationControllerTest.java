package com.aiprreview.controller;

import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.exception.GithubApiException;
import com.aiprreview.exception.GlobalExceptionHandler;
import com.aiprreview.service.GithubService;
import com.aiprreview.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RepositoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "server.servlet.context-path=")
class GithubIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private GithubService githubService;

    @Test
    @WithMockUser(roles = "USER")
    void syncGithubRepositories_ShouldReturnSyncedRepositories() throws Exception {
        RepositoryResponse repo = RepositoryResponse.builder()
                .id("r1")
                .name("hello-world")
                .fullName("octocat/hello-world")
                .owner("octocat")
                .isActive(true)
                .build();

        when(githubService.syncUserRepositories("ghp_test")).thenReturn(List.of(repo));

        mockMvc.perform(post("/repositories/sync/github")
                        .param("token", "ghp_test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully synced repositories from GitHub"))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.repositories[0].id").value("r1"))
                .andExpect(jsonPath("$.data.repositories[0].fullName").value("octocat/hello-world"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void fetchGithubRepository_ShouldReturnCreatedRepository() throws Exception {
        RepositoryResponse repo = RepositoryResponse.builder()
                .id("r99")
                .name("demo")
                .fullName("octocat/demo")
                .owner("octocat")
                .build();

        when(githubService.fetchAndSaveRepository("octocat/demo", null)).thenReturn(repo);

        mockMvc.perform(post("/repositories/sync/github/repo")
                        .param("fullName", "octocat/demo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Repository fetched successfully"))
                .andExpect(jsonPath("$.data.id").value("r99"))
                .andExpect(jsonPath("$.data.fullName").value("octocat/demo"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void syncGithubRepositories_ShouldReturnBadGateway_WhenGithubFails() throws Exception {
        when(githubService.syncUserRepositories(null))
                .thenThrow(new GithubApiException("GitHub API unavailable"));

        mockMvc.perform(post("/repositories/sync/github")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("GitHub API unavailable"));
    }
}
