package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GitHubApiClient}.
 *
 * <p>Uses OkHttp MockWebServer to simulate GitHub API responses and verify:
 * <ul>
 *   <li>Single-page fetch of repositories</li>
 *   <li>Multi-page pagination via the Link header</li>
 *   <li>Error mapping for various HTTP status codes (401, 403, 404, 500)</li>
 *   <li>Correct URL construction for repos and collaborators</li>
 * </ul>
 */
class test_GitHubApiClientTest {

    private MockWebServer mockWebServer;
    private GitHubApiClient gitHubApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash to match how WebClient typically handles base URLs
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        GitHubProperties properties = new GitHubProperties();
        GitHubProperties.Api api = new GitHubProperties.Api();
        api.setPerPage(2); // Small page size for testing pagination
        properties.setApi(api);

        gitHubApiClient = new GitHubApiClient(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // --- Fetch Organization Repos Tests ---

    @Nested
    @DisplayName("Fetch Organization Repos")
    class FetchOrganizationReposTests {

        @Test
        @DisplayName("Should fetch repos from single page successfully")
        void shouldFetchReposSinglePage() throws InterruptedException {
            String responseBody = """
                [
                  {
                    "id": 1,
                    "full_name": "test-org/repo1",
                    "name": "repo1",
                    "private": false,
                    "html_url": "https://github.com/test-org/repo1"
                  },
                  {
                    "id": 2,
                    "full_name": "test-org/repo2",
                    "name": "repo2",
                    "private": true,
                    "html_url": "https://github.com/test-org/repo2"
                  }
                ]
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .setHeader("Content-Type", "application/json"));

            var repos = gitHubApiClient.fetchOrganizationRepos("test-org");

            assertThat(repos).hasSize(2);
            assertThat(repos.get(0).getFullName()).isEqualTo("test-org/repo1");
            assertThat(repos.get(0).getName()).isEqualTo("repo1");
            assertThat(repos.get(0).isPrivate()).isFalse();
            assertThat(repos.get(1).getFullName()).isEqualTo("test-org/repo2");
            assertThat(repos.get(1).isPrivate()).isTrue();

            // Verify the request URL
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("/orgs/test-org/repos");
            assertThat(request.getPath()).contains("per_page=2");
        }

        @Test
        @DisplayName("Should handle pagination with Link header containing next URL")
        void shouldHandlePagination() {
            String page1Body = """
                [
                  {"id": 1, "full_name": "org/repo1", "name": "repo1", "private": false}
                ]
                """;

            String page2Body = """
                [
                  {"id": 2, "full_name": "org/repo2", "name": "repo2", "private": true}
                ]
                """;

            // Page 1 response with Link header pointing to page 2
            String page2Url = mockWebServer.url("/orgs/org/repos?per_page=2&type=all&page=2").toString();
            mockWebServer.enqueue(new MockResponse()
                    .setBody(page1Body)
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Link", "<" + page2Url + ">; rel=\"next\""));

            // Page 2 response (no Link header = last page)
            mockWebServer.enqueue(new MockResponse()
                    .setBody(page2Body)
                    .setHeader("Content-Type", "application/json"));

            var repos = gitHubApiClient.fetchOrganizationRepos("org");

            assertThat(repos).hasSize(2);
            assertThat(repos.get(0).getFullName()).isEqualTo("org/repo1");
            assertThat(repos.get(1).getFullName()).isEqualTo("org/repo2");
        }

        @Test
        @DisplayName("Should handle pagination with multiple Link relationships")
        void shouldHandlePaginationWithMultipleLinks() {
            String page1Body = """
                [
                  {"id": 1, "full_name": "org/repo1", "name": "repo1", "private": false}
                ]
                """;

            String page2Body = """
                [
                  {"id": 2, "full_name": "org/repo2", "name": "repo2", "private": false}
                ]
                """;

            // Link header with both next and last relationships
            String page2Url = mockWebServer.url("/orgs/org/repos?per_page=2&type=all&page=2").toString();
            String lastUrl = mockWebServer.url("/orgs/org/repos?per_page=2&type=all&page=3").toString();
            String linkHeader = "<" + page2Url + ">; rel=\"next\", <" + lastUrl + ">; rel=\"last\"";

            mockWebServer.enqueue(new MockResponse()
                    .setBody(page1Body)
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Link", linkHeader));

            mockWebServer.enqueue(new MockResponse()
                    .setBody(page2Body)
                    .setHeader("Content-Type", "application/json"));

            var repos = gitHubApiClient.fetchOrganizationRepos("org");
            assertThat(repos).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when org has no repos")
        void shouldReturnEmptyListForNoRepos() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("[]")
                    .setHeader("Content-Type", "application/json"));

            var repos = gitHubApiClient.fetchOrganizationRepos("empty-org");
            assertThat(repos).isEmpty();
        }
    }

    // --- Fetch Repo Collaborators Tests ---

    @Nested
    @DisplayName("Fetch Repo Collaborators")
    class FetchRepoCollaboratorsTests {

        @Test
        @DisplayName("Should fetch collaborators successfully")
        void shouldFetchCollaborators() throws InterruptedException {
            String responseBody = """
                [
                  {
                    "id": 100,
                    "login": "alice",
                    "avatar_url": "https://avatars.githubusercontent.com/u/100",
                    "role_name": "admin",
                    "permissions": {
                      "admin": true,
                      "maintain": false,
                      "push": true,
                      "triage": false,
                      "pull": true
                    }
                  },
                  {
                    "id": 101,
                    "login": "bob",
                    "role_name": "write",
                    "permissions": {
                      "admin": false,
                      "maintain": false,
                      "push": true,
                      "triage": false,
                      "pull": true
                    }
                  }
                ]
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .setHeader("Content-Type", "application/json"));

            var collaborators = gitHubApiClient.fetchRepoCollaborators("org", "repo1");

            assertThat(collaborators).hasSize(2);
            assertThat(collaborators.get(0).getLogin()).isEqualTo("alice");
            assertThat(collaborators.get(0).getPermissions().isAdmin()).isTrue();
            assertThat(collaborators.get(1).getLogin()).isEqualTo("bob");
            assertThat(collaborators.get(1).getPermissions().isPush()).isTrue();

            // Verify URL includes affiliation=all
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("/repos/org/repo1/collaborators");
            assertThat(request.getPath()).contains("affiliation=all");
        }

        @Test
        @DisplayName("Should handle paginated collaborators")
        void shouldHandlePaginatedCollaborators() {
            String page1Body = """
                [{"id": 100, "login": "alice"}]
                """;
            String page2Body = """
                [{"id": 101, "login": "bob"}]
                """;

            String page2Url = mockWebServer.url("/repos/org/repo1/collaborators?per_page=2&page=2").toString();
            mockWebServer.enqueue(new MockResponse()
                    .setBody(page1Body)
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Link", "<" + page2Url + ">; rel=\"next\""));

            mockWebServer.enqueue(new MockResponse()
                    .setBody(page2Body)
                    .setHeader("Content-Type", "application/json"));

            var collaborators = gitHubApiClient.fetchRepoCollaborators("org", "repo1");

            assertThat(collaborators).hasSize(2);
            assertThat(collaborators.get(0).getLogin()).isEqualTo("alice");
            assertThat(collaborators.get(1).getLogin()).isEqualTo("bob");
        }
    }

    // --- Error Mapping Tests ---

    @Nested
    @DisplayName("Error Mapping")
    class ErrorMappingTests {

        @Test
        @DisplayName("Should throw GitHubApiException with 401 for unauthorized response")
        void shouldThrowFor401() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setBody("{\"message\": \"Bad credentials\"}")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchOrganizationRepos("org"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(401);
                        assertThat(apiEx.getMessage()).contains("Bad credentials");
                    });
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 403 for forbidden response")
        void shouldThrowFor403() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(403)
                    .setBody("{\"message\": \"API rate limit exceeded\"}")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchOrganizationRepos("org"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 404 for not found organization")
        void shouldThrowFor404() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("{\"message\": \"Not Found\"}")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchOrganizationRepos("nonexistent-org"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 500 for server error")
        void shouldThrowFor500() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("{\"message\": \"Internal Server Error\"}")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchOrganizationRepos("org"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(500);
                    });
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 403 for collaborator fetch forbidden")
        void shouldThrowFor403OnCollaborators() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(403)
                    .setBody("{\"message\": \"Must have push access to view repository collaborators.\"}")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchRepoCollaborators("org", "restricted-repo"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(403);
                        assertThat(apiEx.getMessage()).contains("push access");
                    });
        }

        @Test
        @DisplayName("Should handle empty error response body")
        void shouldHandleEmptyErrorBody() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(502)
                    .setBody("")
                    .setHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> gitHubApiClient.fetchOrganizationRepos("org"))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(ex -> {
                        GitHubApiException apiEx = (GitHubApiException) ex;
                        assertThat(apiEx.getStatusCode()).isEqualTo(502);
                    });
        }
    }
}
