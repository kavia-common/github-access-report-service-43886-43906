package com.example.githubaccessreportbackend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Directly invokes handler methods (without MockMvc) to verify:
 * <ul>
 *   <li>GitHubApiException mapping: 401/403 → 401, 404 → 404, others → 502</li>
 *   <li>ConfigurationException → 500</li>
 *   <li>MissingServletRequestParameterException → 400</li>
 *   <li>Generic Exception → 500 with safe message</li>
 *   <li>Error body structure contains timestamp, status, error, message</li>
 * </ul>
 */
class test_GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // --- GitHubApiException Handling ---

    @Nested
    @DisplayName("GitHubApiException Handling")
    class GitHubApiExceptionTests {

        @Test
        @DisplayName("Should map GitHub 401 to HTTP 401 Unauthorized")
        void shouldMap401ToUnauthorized() {
            GitHubApiException ex = new GitHubApiException("Bad credentials", 401);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).containsEntry("status", 401);
            assertThat(response.getBody()).containsEntry("error", "Unauthorized");
            assertThat(response.getBody()).containsEntry("message", "Bad credentials");
            assertThat(response.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Should map GitHub 403 to HTTP 401 Unauthorized")
        void shouldMap403ToUnauthorized() {
            GitHubApiException ex = new GitHubApiException("API rate limit exceeded", 403);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).containsEntry("status", 401);
            assertThat(response.getBody()).containsEntry("error", "Unauthorized");
            assertThat(response.getBody()).containsEntry("message", "API rate limit exceeded");
        }

        @Test
        @DisplayName("Should map GitHub 404 to HTTP 404 Not Found")
        void shouldMap404ToNotFound() {
            GitHubApiException ex = new GitHubApiException("Not Found", 404);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).containsEntry("status", 404);
            assertThat(response.getBody()).containsEntry("error", "Not Found");
            assertThat(response.getBody()).containsEntry("message", "Not Found");
        }

        @Test
        @DisplayName("Should map GitHub 500 to HTTP 502 Bad Gateway")
        void shouldMap500ToBadGateway() {
            GitHubApiException ex = new GitHubApiException("Internal Server Error", 500);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).containsEntry("status", 502);
            assertThat(response.getBody()).containsEntry("error", "Bad Gateway");
        }

        @Test
        @DisplayName("Should map GitHub 422 to HTTP 502 Bad Gateway (unmapped status)")
        void shouldMap422ToBadGateway() {
            GitHubApiException ex = new GitHubApiException("Validation Failed", 422);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).containsEntry("status", 502);
        }

        @Test
        @DisplayName("Should map GitHub 502 to HTTP 502 Bad Gateway")
        void shouldMapGitHub502ToBadGateway() {
            GitHubApiException ex = new GitHubApiException("Bad Gateway from GitHub", 502);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).containsEntry("status", 502);
            assertThat(response.getBody()).containsEntry("message", "Bad Gateway from GitHub");
        }
    }

    // --- ConfigurationException Handling ---

    @Nested
    @DisplayName("ConfigurationException Handling")
    class ConfigurationExceptionTests {

        @Test
        @DisplayName("Should return 500 Internal Server Error for ConfigurationException")
        void shouldReturn500ForConfigException() {
            ConfigurationException ex = new ConfigurationException(
                    "GitHub Personal Access Token (GITHUB_TOKEN) is not configured.");

            ResponseEntity<Map<String, Object>> response = handler.handleConfigurationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("status", 500);
            assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
            assertThat(response.getBody().get("message").toString()).contains("GITHUB_TOKEN");
            assertThat(response.getBody()).containsKey("timestamp");
        }
    }

    // --- MissingServletRequestParameterException Handling ---

    @Nested
    @DisplayName("MissingServletRequestParameterException Handling")
    class MissingParamTests {

        @Test
        @DisplayName("Should return 400 Bad Request for missing 'org' parameter")
        void shouldReturn400ForMissingOrgParam() throws Exception {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("org", "String");

            ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("status", 400);
            assertThat(response.getBody()).containsEntry("error", "Bad Request");
            assertThat(response.getBody().get("message").toString()).contains("org");
            assertThat(response.getBody().get("message").toString()).contains("missing");
            assertThat(response.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Should include parameter name in error message")
        void shouldIncludeParameterName() throws Exception {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("customParam", "String");

            ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(ex);

            assertThat(response.getBody().get("message").toString()).contains("customParam");
        }
    }

    // --- Generic Exception Handling ---

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 for unexpected RuntimeException")
        void shouldReturn500ForRuntimeException() {
            RuntimeException ex = new RuntimeException("Something unexpected happened");

            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("status", 500);
            assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
            // Should NOT expose internal error details
            assertThat(response.getBody().get("message").toString())
                    .contains("unexpected error");
            assertThat(response.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Should return 500 for NullPointerException")
        void shouldReturn500ForNPE() {
            NullPointerException ex = new NullPointerException("null reference");

            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("status", 500);
        }

        @Test
        @DisplayName("Should return safe generic message that does not leak internal details")
        void shouldReturnSafeGenericMessage() {
            Exception ex = new Exception("Sensitive internal stack trace detail");

            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

            String message = response.getBody().get("message").toString();
            assertThat(message).doesNotContain("Sensitive");
            assertThat(message).contains("unexpected error");
            assertThat(message).contains("try again later");
        }
    }

    // --- Error Body Structure Tests ---

    @Nested
    @DisplayName("Error Body Structure")
    class ErrorBodyStructureTests {

        @Test
        @DisplayName("Error body should always contain timestamp, status, error, and message keys")
        void shouldContainAllRequiredFields() {
            GitHubApiException ex = new GitHubApiException("Test", 404);

            ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

            assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message");
        }

        @Test
        @DisplayName("Timestamp should be a valid ISO-8601 string")
        void shouldHaveValidTimestamp() {
            ConfigurationException ex = new ConfigurationException("Test");

            ResponseEntity<Map<String, Object>> response = handler.handleConfigurationException(ex);

            String timestamp = response.getBody().get("timestamp").toString();
            assertThat(timestamp).isNotNull();
            // ISO-8601 timestamps contain 'T' separator between date and time
            assertThat(timestamp).contains("T");
        }
    }
}
