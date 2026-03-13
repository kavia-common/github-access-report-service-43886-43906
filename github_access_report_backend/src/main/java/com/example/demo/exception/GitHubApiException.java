package com.example.githubaccessreportbackend.exception;

/**
 * Custom exception thrown when a GitHub API call fails.
 * Wraps HTTP status codes and error messages from the GitHub API.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    // PUBLIC_INTERFACE
    /**
     * Constructs a new GitHubApiException.
     *
     * @param message    human-readable error description
     * @param statusCode HTTP status code returned by GitHub
     */
    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a new GitHubApiException with a cause.
     *
     * @param message    human-readable error description
     * @param statusCode HTTP status code returned by GitHub
     * @param cause      the underlying exception
     */
    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code from the failed GitHub API call.
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
