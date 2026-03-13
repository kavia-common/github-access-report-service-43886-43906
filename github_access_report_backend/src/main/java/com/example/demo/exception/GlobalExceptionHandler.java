package com.example.githubaccessreportbackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that converts exceptions into structured JSON error responses.
 */
@RestControllerAdvice(basePackages = "com.example.githubaccessreportbackend")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // PUBLIC_INTERFACE
    /**
     * Handles GitHub API exceptions and returns an appropriate HTTP error response.
     *
     * @param ex the GitHubApiException
     * @return structured error response with details
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiException(GitHubApiException ex) {
        log.error("GitHub API error: {} (status: {})", ex.getMessage(), ex.getStatusCode());

        HttpStatus status;
        if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.getStatusCode() == 404) {
            status = HttpStatus.NOT_FOUND;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }

        return ResponseEntity.status(status).body(buildErrorBody(status, ex.getMessage()));
    }

    // PUBLIC_INTERFACE
    /**
     * Handles configuration exceptions (e.g., missing GitHub token).
     *
     * @param ex the ConfigurationException
     * @return structured error response
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationException(ConfigurationException ex) {
        log.error("Configuration error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    // PUBLIC_INTERFACE
    /**
     * Handles missing required request parameters.
     *
     * @param ex the MissingServletRequestParameterException
     * @return structured error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST,
                        "Required query parameter '" + ex.getParameterName() + "' is missing"));
    }

    // PUBLIC_INTERFACE
    /**
     * Catches all other unexpected exceptions.
     *
     * @param ex the exception
     * @return structured error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please try again later."));
    }

    /**
     * Builds a consistent error response body.
     */
    private Map<String, Object> buildErrorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
