package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.*;
import com.example.githubaccessreportbackend.exception.ConfigurationException;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccessReportService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Configuration validation (missing/blank token)</li>
 *   <li>Report generation with various data scenarios</li>
 *   <li>Concurrent collaborator fetching behavior</li>
 *   <li>User-centric aggregation logic</li>
 *   <li>Permission resolution from permissions object vs. role name</li>
 *   <li>Error propagation from GitHub API client</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class test_AccessReportServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    private GitHubProperties properties;
    private ExecutorService executorService;
    private AccessReportService accessReportService;

    @BeforeEach
    void setUp() {
        // Configure properties with a valid token by default
        properties = new GitHubProperties();
        properties.setToken("ghp_test_token_123");
        GitHubProperties.Api api = new GitHubProperties.Api();
        api.setMaxConcurrency(2);
        api.setPerPage(100);
        properties.setApi(api);

        // Use a small fixed thread pool for tests
        executorService = Executors.newFixedThreadPool(2);

        accessReportService = new AccessReportService(gitHubApiClient, executorService, properties);
    }

    // --- Configuration Validation Tests ---

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("Should throw ConfigurationException when token is null")
        void shouldThrowWhenTokenIsNull() {
            properties.setToken(null);

            assertThatThrownBy(() -> accessReportService.generateReport("test-org"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GITHUB_TOKEN");
        }

        @Test
        @DisplayName("Should throw ConfigurationException when token is empty")
        void shouldThrowWhenTokenIsEmpty() {
            properties.setToken("");

            assertThatThrownBy(() -> accessReportService.generateReport("test-org"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GITHUB_TOKEN");
        }

        @Test
        @DisplayName("Should throw ConfigurationException when token is blank (whitespace)")
        void shouldThrowWhenTokenIsBlank() {
            properties.setToken("   ");

            assertThatThrownBy(() -> accessReportService.generateReport("test-org"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GITHUB_TOKEN");
        }
    }

    // --- Report Generation Tests ---

    @Nested
    @DisplayName("Report Generation")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should return empty report when organization has no repositories")
        void shouldReturnEmptyReportForNoRepos() {
            when(gitHubApiClient.fetchOrganizationRepos("empty-org"))
                    .thenReturn(Collections.emptyList());

            AccessReport report = accessReportService.generateReport("empty-org");

            assertThat(report).isNotNull();
            assertThat(report.getOrganization()).isEqualTo("empty-org");
            assertThat(report.getTotalRepositories()).isZero();
            assertThat(report.getTotalUsers()).isZero();
            assertThat(report.getUsers()).isEmpty();
            assertThat(report.getGeneratedAt()).isNotNull();

            // Should not attempt to fetch collaborators
            verify(gitHubApiClient, never()).fetchRepoCollaborators(anyString(), anyString());
        }

        @Test
        @DisplayName("Should generate report with single repo and single collaborator")
        void shouldGenerateReportSingleRepoSingleCollaborator() {
            RepositoryInfo repo = createRepo(1, "test-org/repo1", "repo1", false);
            CollaboratorInfo collab = createCollaborator("alice", true, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("test-org"))
                    .thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("test-org", "repo1"))
                    .thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("test-org");

            assertThat(report.getOrganization()).isEqualTo("test-org");
            assertThat(report.getTotalRepositories()).isEqualTo(1);
            assertThat(report.getTotalUsers()).isEqualTo(1);
            assertThat(report.getUsers()).hasSize(1);

            UserAccessSummary user = report.getUsers().get(0);
            assertThat(user.getUsername()).isEqualTo("alice");
            assertThat(user.getRepositoryCount()).isEqualTo(1);
            assertThat(user.getRepositories()).hasSize(1);

            RepoAccessEntry entry = user.getRepositories().get(0);
            assertThat(entry.getRepository()).isEqualTo("test-org/repo1");
            assertThat(entry.getPermission()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Should generate report with multiple repos and multiple collaborators")
        void shouldGenerateReportMultipleReposMultipleCollaborators() {
            RepositoryInfo repo1 = createRepo(1, "org/repo1", "repo1", false);
            RepositoryInfo repo2 = createRepo(2, "org/repo2", "repo2", true);

            CollaboratorInfo alice1 = createCollaborator("alice", true, false, false, false, true);
            CollaboratorInfo bob1 = createCollaborator("bob", false, false, true, false, true);
            CollaboratorInfo alice2 = createCollaborator("alice", false, false, true, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org"))
                    .thenReturn(List.of(repo1, repo2));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1"))
                    .thenReturn(List.of(alice1, bob1));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo2"))
                    .thenReturn(List.of(alice2));

            AccessReport report = accessReportService.generateReport("org");

            assertThat(report.getTotalRepositories()).isEqualTo(2);
            assertThat(report.getTotalUsers()).isEqualTo(2);

            // Users should be sorted alphabetically
            assertThat(report.getUsers().get(0).getUsername()).isEqualTo("alice");
            assertThat(report.getUsers().get(1).getUsername()).isEqualTo("bob");

            // Alice should have access to both repos
            UserAccessSummary aliceSummary = report.getUsers().get(0);
            assertThat(aliceSummary.getRepositoryCount()).isEqualTo(2);

            // Bob should have access to only repo1
            UserAccessSummary bobSummary = report.getUsers().get(1);
            assertThat(bobSummary.getRepositoryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should sort users alphabetically by username")
        void shouldSortUsersAlphabetically() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);

            CollaboratorInfo charlie = createCollaborator("charlie", false, false, false, false, true);
            CollaboratorInfo alice = createCollaborator("alice", false, false, false, false, true);
            CollaboratorInfo bob = createCollaborator("bob", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1"))
                    .thenReturn(List.of(charlie, alice, bob));

            AccessReport report = accessReportService.generateReport("org");

            assertThat(report.getUsers()).extracting(UserAccessSummary::getUsername)
                    .containsExactly("alice", "bob", "charlie");
        }

        @Test
        @DisplayName("Should set generated_at timestamp in the report")
        void shouldSetGeneratedTimestamp() {
            when(gitHubApiClient.fetchOrganizationRepos("org"))
                    .thenReturn(Collections.emptyList());

            AccessReport report = accessReportService.generateReport("org");

            assertThat(report.getGeneratedAt()).isNotNull();
            assertThat(report.getGeneratedAt()).isNotEmpty();
        }
    }

    // --- Permission Resolution Tests ---

    @Nested
    @DisplayName("Permission Resolution")
    class PermissionResolutionTests {

        @Test
        @DisplayName("Should resolve admin permission from permissions object")
        void shouldResolveAdminPermission() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = createCollaborator("user", true, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("admin");
        }

        @Test
        @DisplayName("Should resolve write permission from push=true")
        void shouldResolveWritePermission() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = createCollaborator("user", false, false, true, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("write");
        }

        @Test
        @DisplayName("Should resolve maintain permission")
        void shouldResolveMaintainPermission() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = createCollaborator("user", false, true, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("maintain");
        }

        @Test
        @DisplayName("Should resolve read permission from pull=true only")
        void shouldResolveReadPermission() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = createCollaborator("user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("read");
        }

        @Test
        @DisplayName("Should fall back to roleName when permissions object is null")
        void shouldFallBackToRoleName() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = new CollaboratorInfo();
            collab.setLogin("user");
            collab.setPermissions(null);
            collab.setRoleName("admin");

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("admin");
        }

        @Test
        @DisplayName("Should default to 'read' when permissions is null and roleName is blank")
        void shouldDefaultToReadWhenNoPermissionInfo() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo collab = new CollaboratorInfo();
            collab.setLogin("user");
            collab.setPermissions(null);
            collab.setRoleName("");

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getPermission())
                    .isEqualTo("read");
        }

        @Test
        @DisplayName("Should skip collaborators with null or blank login")
        void shouldSkipCollaboratorsWithNullOrBlankLogin() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);
            CollaboratorInfo nullLogin = new CollaboratorInfo();
            nullLogin.setLogin(null);
            CollaboratorInfo blankLogin = new CollaboratorInfo();
            blankLogin.setLogin("   ");
            CollaboratorInfo validUser = createCollaborator("valid-user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1"))
                    .thenReturn(List.of(nullLogin, blankLogin, validUser));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getTotalUsers()).isEqualTo(1);
            assertThat(report.getUsers().get(0).getUsername()).isEqualTo("valid-user");
        }
    }

    // --- Repository Info Handling Tests ---

    @Nested
    @DisplayName("Repository Info Handling")
    class RepositoryInfoHandlingTests {

        @Test
        @DisplayName("Should use fullName when available for repo access entry")
        void shouldUseFullNameWhenAvailable() {
            RepositoryInfo repo = createRepo(1, "org/my-repo", "my-repo", false);
            CollaboratorInfo collab = createCollaborator("user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "my-repo")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getRepository())
                    .isEqualTo("org/my-repo");
        }

        @Test
        @DisplayName("Should fall back to name when fullName is null")
        void shouldFallBackToNameWhenFullNameIsNull() {
            RepositoryInfo repo = new RepositoryInfo();
            repo.setId(1);
            repo.setName("my-repo");
            repo.setFullName(null);
            repo.setPrivate(false);

            CollaboratorInfo collab = createCollaborator("user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "my-repo")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).getRepository())
                    .isEqualTo("my-repo");
        }

        @Test
        @DisplayName("Should correctly set private flag on repo access entry")
        void shouldSetPrivateFlag() {
            RepositoryInfo repo = createRepo(1, "org/private-repo", "private-repo", true);
            CollaboratorInfo collab = createCollaborator("user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "private-repo")).thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");
            assertThat(report.getUsers().get(0).getRepositories().get(0).isPrivate()).isTrue();
        }
    }

    // --- Concurrent Fetching and Error Handling Tests ---

    @Nested
    @DisplayName("Concurrent Fetching and Error Handling")
    class ConcurrentFetchingTests {

        @Test
        @DisplayName("Should gracefully handle 403 on collaborator fetch by returning empty list")
        void shouldHandle403OnCollaboratorFetch() {
            RepositoryInfo repo1 = createRepo(1, "org/repo1", "repo1", false);
            RepositoryInfo repo2 = createRepo(2, "org/repo2", "repo2", false);

            CollaboratorInfo collab = createCollaborator("user", false, false, false, false, true);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo1, repo2));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1"))
                    .thenThrow(new GitHubApiException("Push access required", 403));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo2"))
                    .thenReturn(List.of(collab));

            AccessReport report = accessReportService.generateReport("org");

            assertThat(report.getTotalRepositories()).isEqualTo(2);
            // Only user from repo2 should appear
            assertThat(report.getTotalUsers()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should propagate non-403 GitHubApiException from collaborator fetch")
        void shouldPropagateNon403GitHubApiException() {
            RepositoryInfo repo = createRepo(1, "org/repo1", "repo1", false);

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(List.of(repo));
            when(gitHubApiClient.fetchRepoCollaborators("org", "repo1"))
                    .thenThrow(new GitHubApiException("Not Found", 404));

            assertThatThrownBy(() -> accessReportService.generateReport("org"))
                    .isInstanceOf(GitHubApiException.class);
        }

        @Test
        @DisplayName("Should propagate GitHubApiException from fetchOrganizationRepos")
        void shouldPropagateRepoFetchException() {
            when(gitHubApiClient.fetchOrganizationRepos("bad-org"))
                    .thenThrow(new GitHubApiException("Organization not found", 404));

            assertThatThrownBy(() -> accessReportService.generateReport("bad-org"))
                    .isInstanceOf(GitHubApiException.class)
                    .hasMessageContaining("Organization not found");
        }
    }

    // --- Helper Methods ---

    /**
     * Creates a RepositoryInfo for testing.
     */
    private RepositoryInfo createRepo(long id, String fullName, String name, boolean isPrivate) {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setId(id);
        repo.setFullName(fullName);
        repo.setName(name);
        repo.setPrivate(isPrivate);
        return repo;
    }

    /**
     * Creates a CollaboratorInfo with permissions for testing.
     */
    private CollaboratorInfo createCollaborator(String login, boolean admin, boolean maintain,
                                                 boolean push, boolean triage, boolean pull) {
        CollaboratorInfo collab = new CollaboratorInfo();
        collab.setLogin(login);
        CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
        perms.setAdmin(admin);
        perms.setMaintain(maintain);
        perms.setPush(push);
        perms.setTriage(triage);
        perms.setPull(pull);
        collab.setPermissions(perms);
        return collab;
    }
}
