package com.example.githubaccessreportbackend.controller;

import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.dto.RepoAccessEntry;
import com.example.githubaccessreportbackend.dto.UserAccessSummary;
import com.example.githubaccessreportbackend.exception.ConfigurationException;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import com.example.githubaccessreportbackend.exception.GlobalExceptionHandler;
import com.example.githubaccessreportbackend.service.AccessReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link AccessReportController}.
 *
 * <p>Uses MockMvc in standalone mode with the {@link GlobalExceptionHandler}
 * to verify:
 * <ul>
 *   <li>Successful report generation returns 200 with correct JSON structure</li>
 *   <li>Missing 'org' parameter returns 400</li>
 *   <li>Empty/blank 'org' parameter returns 500 (IllegalArgumentException caught by generic handler)</li>
 *   <li>Service exceptions are properly mapped via GlobalExceptionHandler</li>
 * </ul>
 */
class test_AccessReportControllerTest {

    private MockMvc mockMvc;
    private AccessReportService accessReportService;

    @BeforeEach
    void setUp() {
        accessReportService = mock(AccessReportService.class);
        AccessReportController controller = new AccessReportController(accessReportService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- Successful Request Tests ---

    @Nested
    @DisplayName("Successful Requests")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("Should return 200 with correct report structure")
        void shouldReturn200WithReport() throws Exception {
            RepoAccessEntry entry = new RepoAccessEntry("org/repo1", "admin", false);
            UserAccessSummary user = new UserAccessSummary("alice", List.of(entry));
            AccessReport report = new AccessReport("test-org", 1, List.of(user));

            when(accessReportService.generateReport("test-org")).thenReturn(report);

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "test-org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.organization").value("test-org"))
                    .andExpect(jsonPath("$.total_repositories").value(1))
                    .andExpect(jsonPath("$.total_users").value(1))
                    .andExpect(jsonPath("$.generated_at").isNotEmpty())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users", hasSize(1)))
                    .andExpect(jsonPath("$.users[0].username").value("alice"))
                    .andExpect(jsonPath("$.users[0].repository_count").value(1))
                    .andExpect(jsonPath("$.users[0].repositories[0].repository").value("org/repo1"))
                    .andExpect(jsonPath("$.users[0].repositories[0].permission").value("admin"))
                    .andExpect(jsonPath("$.users[0].repositories[0].private").value(false));

            verify(accessReportService).generateReport("test-org");
        }

        @Test
        @DisplayName("Should return 200 with empty report when no repos found")
        void shouldReturn200WithEmptyReport() throws Exception {
            AccessReport emptyReport = new AccessReport("empty-org", 0, List.of());

            when(accessReportService.generateReport("empty-org")).thenReturn(emptyReport);

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "empty-org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.organization").value("empty-org"))
                    .andExpect(jsonPath("$.total_repositories").value(0))
                    .andExpect(jsonPath("$.total_users").value(0))
                    .andExpect(jsonPath("$.users", hasSize(0)));
        }

        @Test
        @DisplayName("Should trim org parameter before passing to service")
        void shouldTrimOrgParameter() throws Exception {
            AccessReport report = new AccessReport("test-org", 0, List.of());
            when(accessReportService.generateReport("test-org")).thenReturn(report);

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "  test-org  ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(accessReportService).generateReport("test-org");
        }
    }

    // --- Request Validation Tests ---

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 when 'org' parameter is missing")
        void shouldReturn400WhenOrgMissing() throws Exception {
            mockMvc.perform(get("/api/v1/access-report")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("org")));

            verify(accessReportService, never()).generateReport(anyString());
        }

        @Test
        @DisplayName("Should return 500 when 'org' parameter is empty string")
        void shouldReturn500WhenOrgEmpty() throws Exception {
            // Empty string after trim throws IllegalArgumentException,
            // which is caught by the generic exception handler -> 500
            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "   ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));

            verify(accessReportService, never()).generateReport(anyString());
        }
    }

    // --- Exception Handling Integration Tests ---

    @Nested
    @DisplayName("Exception Handling via Controller")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should return 401 when GitHub returns 401 Unauthorized")
        void shouldReturn401ForGitHubAuth() throws Exception {
            when(accessReportService.generateReport("org"))
                    .thenThrow(new GitHubApiException("Bad credentials", 401));

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value(containsString("Bad credentials")));
        }

        @Test
        @DisplayName("Should return 401 when GitHub returns 403 Forbidden")
        void shouldReturn401ForGitHub403() throws Exception {
            when(accessReportService.generateReport("org"))
                    .thenThrow(new GitHubApiException("Forbidden", 403));

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Should return 404 when organization not found on GitHub")
        void shouldReturn404ForNotFoundOrg() throws Exception {
            when(accessReportService.generateReport("nonexistent"))
                    .thenThrow(new GitHubApiException("Not Found", 404));

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "nonexistent")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @DisplayName("Should return 502 for unexpected GitHub API errors")
        void shouldReturn502ForUnexpectedGitHubError() throws Exception {
            when(accessReportService.generateReport("org"))
                    .thenThrow(new GitHubApiException("Server Error", 500));

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.error").value("Bad Gateway"));
        }

        @Test
        @DisplayName("Should return 500 when configuration error (missing token)")
        void shouldReturn500ForConfigError() throws Exception {
            when(accessReportService.generateReport("org"))
                    .thenThrow(new ConfigurationException("GITHUB_TOKEN is not configured"));

            mockMvc.perform(get("/api/v1/access-report")
                            .param("org", "org")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value(containsString("GITHUB_TOKEN")));
        }
    }
}
