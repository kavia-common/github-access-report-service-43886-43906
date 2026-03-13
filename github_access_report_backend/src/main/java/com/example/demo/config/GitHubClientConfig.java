package com.example.githubaccessreportbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration class that creates the WebClient bean used to call the GitHub REST API.
 * The client is pre-configured with the base URL, authentication header, and
 * the recommended Accept header for the GitHub v3 API.
 */
@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubClientConfig.class);

    // PUBLIC_INTERFACE
    /**
     * Creates a {@link WebClient} pre-configured with GitHub API base URL,
     * Bearer token authentication, and JSON accept headers.
     *
     * @param properties the GitHub configuration properties
     * @return a configured WebClient instance for GitHub API calls
     */
    @Bean
    public WebClient gitHubWebClient(GitHubProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .filter(logRequest());

        // Only add authorization header if token is provided
        String token = properties.getToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            log.info("GitHub API client configured with authentication token");
        } else {
            log.warn("No GitHub token configured – API calls will be unauthenticated (lower rate limits)");
        }

        return builder.build();
    }

    /**
     * Logging filter for outgoing requests (debug level).
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("GitHub API request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }
}
