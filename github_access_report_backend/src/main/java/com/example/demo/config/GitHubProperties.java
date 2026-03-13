package com.example.githubaccessreportbackend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for GitHub API integration.
 * Maps properties prefixed with "github" from application.properties or environment variables.
 */
@Validated
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /**
     * GitHub Personal Access Token for authentication.
     * Must be set via the GITHUB_TOKEN environment variable.
     */
    private String token;

    private Api api = new Api();

    // PUBLIC_INTERFACE
    /**
     * Returns the GitHub Personal Access Token.
     *
     * @return the PAT string
     */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the nested API configuration.
     *
     * @return Api configuration object
     */
    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    /**
     * Nested configuration for GitHub API settings.
     */
    public static class Api {

        /** Base URL of the GitHub API (default: https://api.github.com). */
        private String baseUrl = "https://api.github.com";

        /** Maximum number of concurrent requests to GitHub API. */
        @Min(1)
        @Max(50)
        private int maxConcurrency = 10;

        /** Number of items per page for paginated GitHub API calls (max 100). */
        @Min(1)
        @Max(100)
        private int perPage = 100;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        public int getPerPage() {
            return perPage;
        }

        public void setPerPage(int perPage) {
            this.perPage = perPage;
        }
    }
}
