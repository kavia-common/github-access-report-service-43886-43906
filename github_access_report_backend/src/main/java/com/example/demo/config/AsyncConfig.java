package com.example.githubaccessreportbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async configuration providing a thread pool for concurrent GitHub API calls.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    // PUBLIC_INTERFACE
    /**
     * Creates a virtual-thread-based executor service for concurrent GitHub API requests.
     * Virtual threads (Project Loom, Java 19+/preview, Java 21 GA) provide lightweight
     * concurrency ideal for I/O-bound workloads. Falls back to a cached thread pool
     * if virtual threads are not available.
     *
     * @param properties the GitHub configuration properties
     * @return an ExecutorService for concurrent API calls
     */
    @Bean
    public ExecutorService gitHubExecutorService(GitHubProperties properties) {
        // Use a fixed thread pool sized to the configured max concurrency
        return Executors.newFixedThreadPool(properties.getApi().getMaxConcurrency());
    }
}
