package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.*;
import com.example.githubaccessreportbackend.exception.ConfigurationException;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service that orchestrates the generation of GitHub access reports.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Validates that a GitHub token is configured</li>
 *   <li>Fetches all repositories for the given organization</li>
 *   <li>Concurrently fetches collaborators for each repository</li>
 *   <li>Aggregates the data into a user-centric view</li>
 * </ol>
 *
 * <p>Concurrent fetching uses a bounded thread pool to avoid overwhelming
 * the GitHub API while still achieving significant speedup over sequential calls.
 */
@Service
public class AccessReportService {

    private static final Logger log = LoggerFactory.getLogger(AccessReportService.class);

    private final GitHubApiClient gitHubApiClient;
    private final ExecutorService executorService;
    private final GitHubProperties properties;

    /**
     * Constructs the AccessReportService.
     *
     * @param gitHubApiClient    the GitHub API client for fetching data
     * @param executorService    the thread pool for concurrent collaborator fetching
     * @param properties         the GitHub configuration properties
     */
    public AccessReportService(GitHubApiClient gitHubApiClient,
                               ExecutorService executorService,
                               GitHubProperties properties) {
        this.gitHubApiClient = gitHubApiClient;
        this.executorService = executorService;
        this.properties = properties;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates a complete access report for the specified GitHub organization.
     *
     * <p>The report maps each user to the repositories they have access to,
     * along with their permission level on each repository.
     *
     * @param orgName the GitHub organization name (e.g., "my-org")
     * @return an {@link AccessReport} containing aggregated user access data
     * @throws ConfigurationException if the GitHub token is not configured
     * @throws GitHubApiException     if any GitHub API call fails
     */
    public AccessReport generateReport(String orgName) {
        validateConfiguration();

        log.info("Generating access report for organization: {}", orgName);
        long startTime = System.currentTimeMillis();

        // Step 1: Fetch all repositories for the organization
        List<RepositoryInfo> repos = gitHubApiClient.fetchOrganizationRepos(orgName);

        if (repos.isEmpty()) {
            log.info("No repositories found for organization '{}'", orgName);
            return new AccessReport(orgName, 0, Collections.emptyList());
        }

        // Step 2: Concurrently fetch collaborators for each repository
        Map<RepositoryInfo, List<CollaboratorInfo>> repoCollaborators =
                fetchCollaboratorsConcurrently(orgName, repos);

        // Step 3: Aggregate into user-centric view
        List<UserAccessSummary> userSummaries = aggregateUserAccess(repoCollaborators);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Access report generated for '{}' in {}ms: {} repos, {} users",
                orgName, elapsed, repos.size(), userSummaries.size());

        return new AccessReport(orgName, repos.size(), userSummaries);
    }

    /**
     * Validates that the GitHub token is configured before making API calls.
     *
     * @throws ConfigurationException if the token is missing or blank
     */
    private void validateConfiguration() {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            throw new ConfigurationException(
                    "GitHub Personal Access Token (GITHUB_TOKEN) is not configured. "
                    + "Please set the GITHUB_TOKEN environment variable.");
        }
    }

    /**
     * Concurrently fetches collaborators for all repositories using the thread pool.
     * Each repository's collaborator list is fetched in a separate task.
     *
     * @param orgName the organization name
     * @param repos   the list of repositories
     * @return a map of repository to its collaborators
     */
    private Map<RepositoryInfo, List<CollaboratorInfo>> fetchCollaboratorsConcurrently(
            String orgName, List<RepositoryInfo> repos) {

        log.info("Fetching collaborators concurrently for {} repositories", repos.size());

        // Submit all tasks
        Map<RepositoryInfo, CompletableFuture<List<CollaboratorInfo>>> futures = new LinkedHashMap<>();
        for (RepositoryInfo repo : repos) {
            CompletableFuture<List<CollaboratorInfo>> future = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return gitHubApiClient.fetchRepoCollaborators(orgName, repo.getName());
                        } catch (GitHubApiException e) {
                            // If it's a 403 (push access required), log and return empty
                            if (e.getStatusCode() == 403) {
                                log.warn("Cannot access collaborators for {}/{}: {}",
                                        orgName, repo.getName(), e.getMessage());
                                return Collections.<CollaboratorInfo>emptyList();
                            }
                            throw e;
                        }
                    },
                    executorService);
            futures.put(repo, future);
        }

        // Collect results
        Map<RepositoryInfo, List<CollaboratorInfo>> results = new LinkedHashMap<>();
        for (Map.Entry<RepositoryInfo, CompletableFuture<List<CollaboratorInfo>>> entry : futures.entrySet()) {
            try {
                List<CollaboratorInfo> collaborators = entry.getValue().get(60, TimeUnit.SECONDS);
                results.put(entry.getKey(), collaborators);
            } catch (TimeoutException e) {
                log.error("Timeout fetching collaborators for repository: {}", entry.getKey().getName());
                results.put(entry.getKey(), Collections.emptyList());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof GitHubApiException) {
                    throw (GitHubApiException) cause;
                }
                throw new GitHubApiException(
                        "Error fetching collaborators for " + entry.getKey().getName()
                                + ": " + cause.getMessage(), 500, cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GitHubApiException(
                        "Interrupted while fetching collaborators", 500, e);
            }
        }

        return results;
    }

    /**
     * Aggregates repository collaborator data into a user-centric view.
     * For each unique user across all repositories, produces a summary of which
     * repositories they have access to and what their permission level is.
     *
     * @param repoCollaborators map of repository → collaborators
     * @return sorted list of user access summaries
     */
    private List<UserAccessSummary> aggregateUserAccess(
            Map<RepositoryInfo, List<CollaboratorInfo>> repoCollaborators) {

        // Map: username → list of repo access entries
        Map<String, List<RepoAccessEntry>> userRepoMap = new TreeMap<>();

        for (Map.Entry<RepositoryInfo, List<CollaboratorInfo>> entry : repoCollaborators.entrySet()) {
            RepositoryInfo repo = entry.getKey();
            List<CollaboratorInfo> collaborators = entry.getValue();

            for (CollaboratorInfo collaborator : collaborators) {
                String username = collaborator.getLogin();
                if (username == null || username.isBlank()) {
                    continue;
                }

                // Determine the permission level
                String permission = "read"; // default
                if (collaborator.getPermissions() != null) {
                    permission = collaborator.getPermissions().toPermissionLevel();
                } else if (collaborator.getRoleName() != null && !collaborator.getRoleName().isBlank()) {
                    permission = collaborator.getRoleName();
                }

                RepoAccessEntry accessEntry = new RepoAccessEntry(
                        repo.getFullName() != null ? repo.getFullName() : repo.getName(),
                        permission,
                        repo.isPrivate());

                userRepoMap.computeIfAbsent(username, k -> new ArrayList<>()).add(accessEntry);
            }
        }

        // Convert to list of UserAccessSummary sorted by username
        return userRepoMap.entrySet().stream()
                .map(entry -> new UserAccessSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(UserAccessSummary::getUsername))
                .collect(Collectors.toList());
    }
}
