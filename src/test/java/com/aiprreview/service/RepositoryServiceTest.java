package com.aiprreview.service;

import com.aiprreview.dto.repository.RepositoryRequest;
import com.aiprreview.dto.repository.RepositoryResponse;
import com.aiprreview.entity.RepositoryEntity;
import com.aiprreview.entity.User;
import com.aiprreview.exception.ResourceNotFoundException;
import com.aiprreview.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RepositoryService repositoryService;

    @Test
    void addRepository_ShouldCreateWithDefaults_WhenOptionalFieldsAreNull() {
        User currentUser = User.builder()
                .id("u1")
                .username("alice")
                .build();
        RepositoryRequest request = RepositoryRequest.builder()
                .fullName("octocat/hello-world")
                .description("test repository")
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.existsByUserIdAndFullName("u1", "octocat/hello-world")).thenReturn(false);
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenAnswer(invocation -> {
            RepositoryEntity entity = invocation.getArgument(0);
            entity.setId("r1");
            return entity;
        });

        RepositoryResponse response = repositoryService.addRepository(request);

        assertEquals("r1", response.getId());
        assertEquals("octocat", response.getOwner());
        assertEquals("hello-world", response.getName());
        assertEquals("https://github.com/octocat/hello-world", response.getUrl());
        assertEquals("main", response.getDefaultBranch());
        assertFalse(response.getWebhookEnabled());

        ArgumentCaptor<RepositoryEntity> captor = ArgumentCaptor.forClass(RepositoryEntity.class);
        verify(repositoryRepository).save(captor.capture());
        RepositoryEntity toSave = captor.getValue();
        assertEquals("u1", toSave.getUserId());
        assertTrue(toSave.getIsActive());
        assertNotNull(toSave.getCreatedAt());
        assertNotNull(toSave.getUpdatedAt());
    }

    @Test
    void addRepository_ShouldThrow_WhenAlreadyExistsForUser() {
        User currentUser = User.builder().id("u1").build();
        RepositoryRequest request = RepositoryRequest.builder().fullName("octocat/hello-world").build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.existsByUserIdAndFullName("u1", "octocat/hello-world")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> repositoryService.addRepository(request));

        assertEquals("Repository 'octocat/hello-world' already exists", ex.getMessage());
        verify(repositoryRepository, never()).save(any(RepositoryEntity.class));
    }

    @Test
    void getRepositoryById_ShouldThrow_WhenNotFoundForCurrentUser() {
        User currentUser = User.builder().id("u1").build();
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByIdAndUserId("r1", "u1")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> repositoryService.getRepositoryById("r1"));

        assertEquals("Repository not found with id: r1", ex.getMessage());
    }

    @Test
    void updateRepository_ShouldUpdateOnlyProvidedFields() {
        User currentUser = User.builder().id("u1").username("alice").build();
        RepositoryEntity existing = RepositoryEntity.builder()
                .id("r1")
                .userId("u1")
                .name("hello-world")
                .fullName("octocat/hello-world")
                .owner("octocat")
                .url("https://github.com/octocat/hello-world")
                .description("old")
                .defaultBranch("main")
                .webhookEnabled(false)
                .isActive(true)
                .build();

        RepositoryRequest request = RepositoryRequest.builder()
                .description("new description")
                .enableWebhook(true)
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByIdAndUserId("r1", "u1")).thenReturn(Optional.of(existing));
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepositoryResponse response = repositoryService.updateRepository("r1", request);

        assertEquals("new description", response.getDescription());
        assertEquals("main", response.getDefaultBranch());
        assertTrue(response.getWebhookEnabled());
    }

    @Test
    void toggleRepositoryStatus_ShouldFlipActiveFlag() {
        User currentUser = User.builder().id("u1").build();
        RepositoryEntity existing = RepositoryEntity.builder()
                .id("r1")
                .userId("u1")
                .fullName("octocat/hello-world")
                .isActive(true)
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByIdAndUserId("r1", "u1")).thenReturn(Optional.of(existing));
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepositoryResponse response = repositoryService.toggleRepositoryStatus("r1");

        assertFalse(response.getIsActive());
    }

    @Test
    void deleteRepository_ShouldDeleteEntity_WhenFound() {
        User currentUser = User.builder().id("u1").build();
        RepositoryEntity existing = RepositoryEntity.builder().id("r1").userId("u1").build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByIdAndUserId("r1", "u1")).thenReturn(Optional.of(existing));

        repositoryService.deleteRepository("r1");

        verify(repositoryRepository).delete(existing);
    }

    @Test
    void getAllRepositories_ShouldMapEntitiesToResponses() {
        User currentUser = User.builder().id("u1").username("alice").build();
        RepositoryEntity first = RepositoryEntity.builder().id("r1").name("a").fullName("o/a").owner("o").isActive(true).build();
        RepositoryEntity second = RepositoryEntity.builder().id("r2").name("b").fullName("o/b").owner("o").isActive(false).build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByUserId("u1")).thenReturn(List.of(first, second));

        List<RepositoryResponse> responses = repositoryService.getAllRepositories();

        assertEquals(2, responses.size());
        assertEquals("r1", responses.get(0).getId());
        assertEquals("r2", responses.get(1).getId());
    }

    @Test
    void searchRepositories_ShouldReturnFilteredResultsForCurrentUser() {
        User currentUser = User.builder().id("u1").build();
        RepositoryEntity match = RepositoryEntity.builder().id("r1").name("hello-world").fullName("octocat/hello-world").build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.findByUserIdAndNameContainingIgnoreCase("u1", "hello"))
                .thenReturn(List.of(match));

        List<RepositoryResponse> responses = repositoryService.searchRepositories("hello");

        assertEquals(1, responses.size());
        assertEquals("r1", responses.get(0).getId());
    }

    @Test
    void getRepositoryCount_ShouldReturnCountForCurrentUser() {
        User currentUser = User.builder().id("u1").build();
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(repositoryRepository.countByUserId("u1")).thenReturn(4L);

        Long count = repositoryService.getRepositoryCount();

        assertEquals(4L, count);
    }

    private static void assertNotNull(Object value) {
        org.junit.jupiter.api.Assertions.assertNotNull(value);
    }
}
